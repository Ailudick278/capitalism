package com.ailudick.capitalismmod.loan;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyFinancialSnapshot;
import com.ailudick.capitalismmod.company.CompanyQualityHoldSavedData;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.util.EconomyMath;
import net.minecraft.server.MinecraftServer;

/** Read-only conservative collateral view used to prepare secured lending. */
public record CompanyCollateralAssessment(long inventoryValue, long restrictedInventoryValue,
                                          long equipmentValue,
                                          long eligibleInventory, long eligibleEquipment,
                                          long eligibleCollateral, long existingDebt,
                                          long indicativeHeadroom) {
    private static final long INVENTORY_HAIRCUT_PERCENT = 50L;
    private static final long EQUIPMENT_HAIRCUT_PERCENT = 60L;

    public static CompanyCollateralAssessment from(MinecraftServer server, Company company) {
        CompanyFinancialSnapshot snapshot = CompanyFinancialSnapshot.from(server, company);
        long restricted = restrictedInventoryValue(server, company);
        long usableInventory = Math.max(0L, snapshot.inventory() - Math.min(snapshot.inventory(), restricted));
        long eligibleInventory = percentage(usableInventory, INVENTORY_HAIRCUT_PERCENT);
        long eligibleEquipment = percentage(snapshot.equipment(), EQUIPMENT_HAIRCUT_PERCENT);
        long eligible = add(eligibleInventory, eligibleEquipment);
        long debt = Math.max(0L, snapshot.loanLiabilities());
        long headroom = Math.max(0L, eligible - debt);
        return new CompanyCollateralAssessment(snapshot.inventory(), restricted, snapshot.equipment(),
                eligibleInventory, eligibleEquipment, eligible, debt, headroom);
    }

    private static long restrictedInventoryValue(MinecraftServer server, Company company) {
        if (server == null || company == null) return 0L;
        var warehouse = WarehouseSavedData.get(server);
        var owner = InventoryOwner.company(company.companyId());
        var holds = CompanyQualityHoldSavedData.get(server);
        var commodities = CommoditySavedData.get(server);
        long total = 0L;
        for (var entry : warehouse.storage(owner).entrySet()) {
            int held = Math.min(Math.max(0, entry.getValue()), holds.heldUnits(company.companyId(), entry.getKey()));
            long unitPrice = Math.max(0L, commodities.price(entry.getKey()));
            long value = EconomyMath.multiply(unitPrice, held);
            total = EconomyMath.add(total, value);
            if (total < 0L) return Long.MAX_VALUE;
        }
        return Math.max(0L, total);
    }

    private static long percentage(long amount, long percent) {
        if (amount <= 0L || percent <= 0L) return 0L;
        if (amount > Long.MAX_VALUE / percent) return Long.MAX_VALUE;
        return amount * percent / 100L;
    }

    private static long add(long left, long right) {
        long result = EconomyMath.add(Math.max(0L, left), Math.max(0L, right));
        return result < 0L ? Long.MAX_VALUE : result;
    }
}
