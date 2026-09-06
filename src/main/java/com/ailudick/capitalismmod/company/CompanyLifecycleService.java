package com.ailudick.capitalismmod.company;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.ailudick.capitalismmod.loan.CompanyLoanSavedData;
import com.ailudick.capitalismmod.loan.CompanyLoan;
import com.ailudick.capitalismmod.loan.CompanyLoanHelper;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import com.ailudick.capitalismmod.economy.EconomySavedData;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.ailudick.capitalismmod.economy.labor.LaborMarketService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Applies the small legal-status state machine currently supported by companies. */
public final class CompanyLifecycleService {
    private CompanyLifecycleService() {
    }

    public static boolean canOperate(MinecraftServer server, String companyId) {
        return server != null && "ACTIVE".equals(CompanyStatusSavedData.get(server).statusOf(companyId));
    }

    public static String status(MinecraftServer server, String companyId) {
        return CompanyStatusSavedData.get(server).statusOf(companyId);
    }

    public static boolean suspend(ServerPlayer player, Company company, String reason) {
        if (player == null || company == null || !company.ownerUuid().equals(player.getUUID())) return false;
        CompanyStatusSavedData data = CompanyStatusSavedData.get(player.getServer());
        if (!"ACTIVE".equals(data.statusOf(company.companyId()))) return false;
        long now = player.getServer().overworld().getGameTime();
        data.set(new CompanyStatusSavedData.Status(company.companyId(), "SUSPENDED", now,
                reason == null ? "" : reason));
        LaborMarketService.endEmploymentsForCompany(player.getServer(), company.companyId(), now);
        return true;
    }

    public static boolean resume(ServerPlayer player, Company company) {
        if (player == null || company == null || !company.ownerUuid().equals(player.getUUID())) return false;
        CompanyStatusSavedData data = CompanyStatusSavedData.get(player.getServer());
        if (!"SUSPENDED".equals(data.statusOf(company.companyId()))) return false;
        if (TaxService.outstanding(player.getServer(), new TaxSubject(TaxType.CORPORATE_INCOME,
                company.companyId(), company.ownerUuid())) > 0L) return false;
        if (CompanyLoanSavedData.get(player.getServer()).forCompany(company.companyId()).stream()
                .anyMatch(loan -> loan.daysRemaining() < 0)) return false;
        if (CompanyPayrollSavedData.get(player.getServer()).unpaid(company.companyId()) > 0L) return false;
        data.set(new CompanyStatusSavedData.Status(company.companyId(), "ACTIVE",
                player.getServer().overworld().getGameTime(), "resumed by owner"));
        return true;
    }

    public static boolean forceSuspend(MinecraftServer server, String companyId, String reason) {
        if (server == null || companyId == null || companyId.isBlank()) return false;
        CompanyStatusSavedData data = CompanyStatusSavedData.get(server);
        if (!"ACTIVE".equals(data.statusOf(companyId))) return false;
        long now = server.overworld().getGameTime();
        data.set(new CompanyStatusSavedData.Status(companyId, "SUSPENDED", now,
                reason == null ? "regulatory suspension" : reason));
        LaborMarketService.endEmploymentsForCompany(server, companyId, now);
        return true;
    }

    /** Opens liquidation after a prolonged default without requiring the owner to be online. */
    public static boolean forceLiquidation(MinecraftServer server, String companyId, String reason) {
        if (server == null || companyId == null || companyId.isBlank()) return false;
        CompanyStatusSavedData data = CompanyStatusSavedData.get(server);
        String current = data.statusOf(companyId);
        if ("LIQUIDATING".equals(current) || "DISSOLVED".equals(current)
                || EconomySavedData.get(server).isListed(companyId)) return false;
        long now = server.overworld().getGameTime();
        data.set(new CompanyStatusSavedData.Status(companyId, "LIQUIDATING", now,
                reason == null ? "automatic liquidation after loan default" : reason));
        LaborMarketService.endEmploymentsForCompany(server, companyId, now);
        return true;
    }

    public static boolean beginLiquidation(ServerPlayer player, Company company) {
        if (player == null || company == null || !company.ownerUuid().equals(player.getUUID())) return false;
        CompanyStatusSavedData data = CompanyStatusSavedData.get(player.getServer());
        String current = data.statusOf(company.companyId());
        if ("LIQUIDATING".equals(current) || "DISSOLVED".equals(current)
                || EconomySavedData.get(player.getServer()).isListed(company.companyId())) return false;
        long now = player.getServer().overworld().getGameTime();
        data.set(new CompanyStatusSavedData.Status(company.companyId(), "LIQUIDATING", now,
                "liquidation opened by owner"));
        LaborMarketService.endEmploymentsForCompany(player.getServer(), company.companyId(), now);
        return true;
    }

    /** Converts recoverable assets and pays priority liabilities; may remain partially settled. */
    public static boolean settleLiquidation(ServerPlayer player, Company company) {
        if (player == null || company == null || !company.ownerUuid().equals(player.getUUID())
                || !"LIQUIDATING".equals(status(player.getServer(), company.companyId()))) return false;
        MinecraftServer server = player.getServer();
        long recovered = liquidateInventory(server, company);
        recovered = EconomyMath.add(recovered, liquidateEquipment(server, company));
        if (recovered > 0L) {
            CompanyHelper.creditTreasuryNonOperating(server, company.companyId(), Currencies.USD.id(), recovered,
                    "liquidation_asset_recovery", "清算资产变现");
        }

        Company current = CompanySavedData.get(server).get(company.companyId());
        if (current == null) return false;
        CompanyPayrollService.payOutstanding(server, current);
        current = CompanySavedData.get(server).get(company.companyId());
        if (current == null || CompanyPayrollSavedData.get(server).unpaid(company.companyId()) > 0L) return false;
        TaxSubject subject = new TaxSubject(TaxType.CORPORATE_INCOME, current.companyId(), current.ownerUuid());
        long taxMinor = TaxService.outstanding(server, subject);
        if (taxMinor > 0L) {
            long taxMajor = Money.toMajorCeiling(taxMinor);
            long pay = Math.min(current.treasuryOf(Currencies.USD.id()), taxMajor);
            if (pay > 0L && CompanyHelper.debitTreasuryNonOperating(server, current.companyId(), Currencies.USD.id(),
                    pay, "liquidation_tax_payment", "清算优先清偿税款")) {
                TaxService.settleFromProceeds(server, subject, Money.toMinorSaturated(pay),
                        "liquidation-tax:" + current.companyId() + ":" + server.overworld().getGameTime(),
                        server.overworld().getGameTime());
            }
        }

        current = CompanySavedData.get(server).get(company.companyId());
        for (CompanyLoan loan : CompanyLoanSavedData.get(server).forCompany(company.companyId())) {
            current = CompanySavedData.get(server).get(company.companyId());
            if (current == null || current.treasuryOf(Currencies.USD.id()) <= 0L) break;
            long total = EconomyMath.add(loan.principal(), loan.interestDue());
            long payment = Math.min(current.treasuryOf(Currencies.USD.id()), total);
            if (payment > 0L) CompanyLoanHelper.repay(player, current.name(), loan.id(), payment);
        }

        current = CompanySavedData.get(server).get(company.companyId());
        if (current == null || TaxService.outstanding(server, subject) > 0L
                || !CompanyLoanSavedData.get(server).forCompany(company.companyId()).isEmpty()) return false;
        long remaining = current.treasuryOf(Currencies.USD.id());
        if (remaining > 0L) CompanyHelper.withdraw(player, current.name(), Currencies.USD.id(), remaining);
        CompanyLaborSavedData.get(server).clear(company.companyId());
        CompanySiteAllocationSavedData.get(server).remove(company.companyId());
        CompanySiteSavedData.get(server).remove(company.companyId());
        CompanyStatusSavedData.get(server).set(new CompanyStatusSavedData.Status(company.companyId(), "DISSOLVED",
                server.overworld().getGameTime(), "liquidation completed"));
        return true;
    }

    private static long liquidateInventory(MinecraftServer server, Company company) {
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        CommoditySavedData market = CommoditySavedData.get(server);
        long recovered = 0L;
        for (var entry : new java.util.HashMap<>(warehouse.storage(InventoryOwner.company(company.companyId()))).entrySet()) {
            Item item = parseItem(entry.getKey());
            if (item == null || !warehouse.consume(InventoryOwner.company(company.companyId()), item, entry.getValue())) continue;
            CompanyInventoryCostSavedData.get(server).consume(company.companyId(), entry.getKey(), entry.getValue());
            long value = EconomyMath.multiply(Math.max(0L, market.price(entry.getKey())), entry.getValue());
            recovered = addRecovered(recovered, value);
        }
        return applyRecoveryRate(recovered);
    }

    private static long liquidateEquipment(MinecraftServer server, Company company) {
        CompanyEquipmentSavedData equipment = CompanyEquipmentSavedData.get(server);
        long recovered = 0L;
        for (var entry : equipment.all(company.companyId()).values()) {
            MachineType type = MachineType.parse(entry.machineType());
            if (type == null || entry.count() <= 0 || entry.condition() <= 0) continue;
            long value = EconomyMath.multiply(type.purchasePrice(), entry.count());
            value = EconomyMath.multiply(value, entry.condition()) / 100L;
            if (equipment.remove(company.companyId(), type, entry.count())) recovered = addRecovered(recovered, value);
        }
        return applyRecoveryRate(recovered);
    }

    private static long applyRecoveryRate(long value) {
        if (value <= 0L) return 0L;
        double rate = Config.COMPANY_LIQUIDATION_RECOVERY_RATE.get();
        if (!Double.isFinite(rate) || rate <= 0.0) return 0L;
        if (rate >= 1.0 || value >= Long.MAX_VALUE / rate) return rate >= 1.0 ? value : Long.MAX_VALUE;
        return Math.max(0L, (long) Math.floor(value * rate));
    }

    private static long addRecovered(long left, long right) {
        return right <= 0L || left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static Item parseItem(String itemId) {
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            return item == null || item == Items.AIR ? null : item;
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
