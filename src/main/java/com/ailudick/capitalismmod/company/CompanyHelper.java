package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.economy.EconomySavedData;
import com.ailudick.capitalismmod.init.ModAttachments;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.supply.SupplyMarket;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import com.ailudick.capitalismmod.tax.TaxableIncomeEvent;
import com.ailudick.capitalismmod.tax.CorporateTaxPeriodSavedData;
import com.ailudick.capitalismmod.tax.CorporateTaxAnnualSavedData;
import com.ailudick.capitalismmod.tax.TaxTransactionService;
import com.ailudick.capitalismmod.tax.TaxExpenseService;
import com.ailudick.capitalismmod.loan.CompanyLoanSavedData;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.data.CapitalismData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central helpers for a player's conglomerate and its companies.
 * Company identity/financials stay per-player; a listed company additionally
 * registers a stock in {@link EconomySavedData} so other players can trade it.
 */
public final class CompanyHelper {
    private CompanyHelper() {
    }

    public static Conglomerate getConglomerate(Player player) {
        return player.getData(ModAttachments.CONGLOMERATE);
    }

    public static Map<String, Company> getCompanies(Player player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return Map.of();
        }
        CompanySavedData registry = CompanySavedData.get(server);
        Map<String, Company> result = new HashMap<>();
        for (Map.Entry<String, String> entry : getConglomerate(player).companies().entrySet()) {
            Company company = registry.get(entry.getValue());
            if (company != null) {
                result.put(entry.getKey(), company);
            }
        }
        return result;
    }

    public static Company getCompany(Player player, String name) {
        return getCompanies(player).get(name);
    }

    /** Looks up a company by its durable owner UUID and name for offline market records. */
    public static Company findCompany(MinecraftServer server, UUID ownerUuid, String name) {
        if (server == null || ownerUuid == null || name == null) return null;
        for (Company company : CompanySavedData.get(server).companies().values()) {
            if (ownerUuid.equals(company.ownerUuid()) && name.equals(company.name())) return company;
        }
        return null;
    }

    /** Credits company revenue without requiring its legal owner to be online. */
    public static boolean creditTreasury(MinecraftServer server, String companyId, String currencyId, long amount) {
        if (server == null || companyId == null || currencyId == null || amount <= 0) return false;
        CompanySavedData data = CompanySavedData.get(server);
        Company company = data.get(companyId);
        if (company == null) return false;
        Company updated = company.addTreasury(currencyId, amount);
        if (updated == company) return false;
        data.put(updated);
        CompanyLedgerSavedData.get(server).append(new CompanyLedgerEntry(
                company.companyId(), server.overworld().getGameTime(), "revenue", currencyId,
                amount, updated.treasuryOf(currencyId), "供应市场销售收入"));
        return true;
    }

    /** Credits a non-operating financing inflow without labeling it revenue. */
    public static boolean creditTreasuryNonOperating(MinecraftServer server, String companyId,
                                                       String currencyId, long amount,
                                                       String type, String description) {
        if (server == null || companyId == null || currencyId == null || amount <= 0L) return false;
        CompanySavedData data = CompanySavedData.get(server);
        Company company = data.get(companyId);
        if (company == null) return false;
        Company updated = company.addTreasury(currencyId, amount);
        if (updated == company) return false;
        data.put(updated);
        CompanyLedgerSavedData.get(server).append(new CompanyLedgerEntry(
                company.companyId(), server.overworld().getGameTime(), type, currencyId,
                amount, updated.treasuryOf(currencyId), description));
        return true;
    }

    /** Debits a company's treasury and records the expense before production continues. */
    public static boolean debitTreasury(MinecraftServer server, String companyId, String currencyId,
                                        long amount, String type, String description) {
        return debitTreasuryInternal(server, companyId, currencyId, amount, type, description, true);
    }

    /** Debits cash for financing or other balance-sheet transactions, not a tax-deductible expense. */
    public static boolean debitTreasuryNonOperating(MinecraftServer server, String companyId,
                                                     String currencyId, long amount,
                                                     String type, String description) {
        return debitTreasuryInternal(server, companyId, currencyId, amount, type, description, false);
    }

    private static boolean debitTreasuryInternal(MinecraftServer server, String companyId, String currencyId,
                                                 long amount, String type, String description,
                                                 boolean taxableExpense) {
        if (server == null || companyId == null || currencyId == null || amount < 0L) return false;
        CompanySavedData data = CompanySavedData.get(server);
        Company company = data.get(companyId);
        if (company == null || company.treasuryOf(currencyId) < amount) return false;
        Map<String, Long> treasury = new HashMap<>(company.treasury());
        long balance = company.treasuryOf(currencyId) - amount;
        treasury.put(currencyId, balance);
        Company updated = company.withTreasury(treasury);
        data.put(updated);
        if (amount > 0L) {
            CompanyLedgerSavedData.get(server).append(new CompanyLedgerEntry(
                    company.companyId(), server.overworld().getGameTime(), type, currencyId,
                    -amount, balance, description));
            if (taxableExpense) {
                recordTaxableExpense(server, company, type + ":" + server.overworld().getGameTime(),
                        amount, currencyId, server.overworld().getGameTime());
            }
        }
        return true;
    }

    /** Records a confirmed company revenue event and assesses corporate income tax once. */
    public static boolean recordTaxableIncome(MinecraftServer server, Company company, String sourceId,
                                              long revenue, String currencyId, long occurredAt) {
        if (server == null || company == null || sourceId == null || sourceId.isBlank() || revenue <= 0L) return false;
        long quarter = PerpetualCalendar.ticksForDays(90L);
        long periodEnd = ((occurredAt / quarter) + 1L) * quarter;
        long periodStart = periodEnd - quarter;
        CorporateTaxPeriodSavedData.get(server).record(company.companyId() + ":" + sourceId,
                company.companyId(), currencyId, revenue, periodStart, periodEnd);
        long year = PerpetualCalendar.ticksForDays(360L);
        long yearEnd = ((occurredAt / year) + 1L) * year;
        CorporateTaxAnnualSavedData.get(server).record(company.companyId(), currencyId, revenue,
                yearEnd - year, yearEnd);
        return true;
    }

    /** Records a deductible business expense for the same corporate tax periods. */
    public static boolean recordTaxableExpense(MinecraftServer server, Company company, String sourceId,
                                                long expense, String currencyId, long occurredAt) {
        if (server == null || company == null || sourceId == null || sourceId.isBlank() || expense <= 0L) return false;
        long quarter = PerpetualCalendar.ticksForDays(90L);
        long periodEnd = ((occurredAt / quarter) + 1L) * quarter;
        long periodStart = periodEnd - quarter;
        CorporateTaxPeriodSavedData.get(server).recordExpense(company.companyId() + ":expense:" + sourceId,
                company.companyId(), currencyId, expense, periodStart, periodEnd);
        long year = PerpetualCalendar.ticksForDays(360L);
        long yearEnd = ((occurredAt / year) + 1L) * year;
        CorporateTaxAnnualSavedData.get(server).recordExpense(company.companyId(), currencyId, expense,
                yearEnd - year, yearEnd);
        TaxExpenseService.record(server, company.ownerUuid(), company.companyId(), "business_expense",
                currencyId, expense, occurredAt, company.companyId() + ":expense:" + sourceId, true);
        return true;
    }

    /** Records a non-cash inventory impairment without charging the treasury again. */
    public static boolean recordInventoryLoss(MinecraftServer server, String companyId, long amount, String shipmentId) {
        if (server == null || companyId == null || companyId.isBlank() || amount <= 0L
                || shipmentId == null || shipmentId.isBlank()) return false;
        Company company = CompanySavedData.get(server).get(companyId);
        if (company == null) return false;
        CompanyLedgerSavedData.get(server).append(new CompanyLedgerEntry(company.companyId(),
                server.overworld().getGameTime(), "inventory_loss", Currencies.USD.id(), -amount,
                company.treasuryOf(Currencies.USD.id()), "运输损失：shipment " + shipmentId));
        return true;
    }

    public static boolean exists(Player player, String name) {
        return getCompanies(player).containsKey(name);
    }

    public static String stockId(Player player, String name) {
        Company company = getCompany(player, name);
        return company == null ? player.getStringUUID() + ":" + name : company.companyId();
    }

    public static boolean isListed(Player player, String name) {
        MinecraftServer server = player.getServer();
        return server != null && EconomySavedData.get(server).isListed(stockId(player, name));
    }

    /** Creates a company in the player's conglomerate. Name is trimmed and must be unique and ≤ 32 chars. */
    public static boolean create(Player player, String name, String type) {
        if (!CompanyTypes.isValid(type) || name == null) {
            return false;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 32) {
            return false;
        }
        Conglomerate conglomerate = getConglomerate(player);
        if (conglomerate.companies().containsKey(trimmed)) {
            return false;
        }

        Map<String, String> updated = new HashMap<>(conglomerate.companies());
        Company company = Company.create(trimmed, type, player.getUUID());
        updated.put(trimmed, company.companyId());
        player.setData(ModAttachments.CONGLOMERATE, new Conglomerate(conglomerate.name(), updated));
        if (player.getServer() != null) {
            CompanySavedData.get(player.getServer()).put(company);
        }
        return true;
    }

    /** Renames the player's conglomerate. Name is trimmed and must be non-empty and ≤ 32 chars. */
    public static boolean rename(Player player, String newName) {
        if (newName == null) {
            return false;
        }
        String trimmed = newName.trim();
        if (trimmed.isEmpty() || trimmed.length() > 32) {
            return false;
        }
        Conglomerate conglomerate = getConglomerate(player);
        if (trimmed.equals(conglomerate.name())) {
            return false;
        }
        player.setData(ModAttachments.CONGLOMERATE, new Conglomerate(trimmed, conglomerate.companies()));
        return true;
    }

    /**
     * Automatic industry recipes are intentionally disabled. Company revenue and
     * production will come from orders/contracts rather than a fixed recipe.
     */
    public static void accrueIncome(Player player) {
        // Kept as an integration hook for the future order settlement service.
    }

    /** Runs one atomic recipe batch: all inputs are checked before any are consumed. */
    public static boolean runProductionCycle(MinecraftServer server, Company company) {
        if (server == null || company == null || company.registeredCapital() <= 0
                || CompanyEconomy.outputs(company).isEmpty() || !canProduceOutputs(server, company)) {
            return false;
        }
        if (!CompanyLifecycleService.canOperate(server, company.companyId())) return false;
        ProductionRecipe recipe = CompanyEconomy.recipe(company);
        if (recipe == null) return false;
        MachineType machine = MachineType.parse(recipe.machineType());
        if (machine == null) return false;
        CompanyLaborSavedData labor = CompanyLaborSavedData.get(server);
        if (recipe.workersPerCycle() > 0 && labor.activeWorkers(company.companyId()) < recipe.workersPerCycle()) {
            return false;
        }
        if (machine != MachineType.NONE
                && CompanyEquipmentSavedData.get(server).count(company.companyId(), machine)
                <= 0) {
            return false;
        }
        long machineCost = machine == MachineType.NONE ? 0L : machine.maintenancePerCycle();
        long cost;
        try {
            long operatingOverhead = Config.COMPANY_FIXED_OVERHEAD_PER_CYCLE.get();
            cost = Math.addExact(Math.addExact(Math.addExact(0L, Math.max(0L, recipe.energyCost())),
                            Math.max(0L, recipe.maintenanceCost())),
                    Math.addExact(machineCost, operatingOverhead));
        } catch (ArithmeticException e) {
            return false;
        }
        if (!consumeInputsPreview(server, company) || !debitTreasury(server, company.companyId(),
                Currencies.USD.id(), cost, "production_expense", "生产周期劳动力、能源与设备维护成本")) {
            return false;
        }
        if (!consumeInputs(server, company)) {
            return false;
        }
        produceOutputs(server, company);
        CompanyEquipmentSavedData.get(server).use(company.companyId(), machine);
        return true;
    }

    /**
     * Number of batches that can be processed in parallel by the active
     * workforce and functioning equipment for the selected recipe.
     */
    public static int parallelCapacity(MinecraftServer server, Company company) {
        if (server == null || company == null) return 0;
        ProductionRecipe recipe = CompanyEconomy.recipe(company);
        if (recipe == null) return 0;
        MachineType machine = MachineType.parse(recipe.machineType());
        if (machine == null) return 0;
        int machineCapacity = machine == MachineType.NONE ? 1
                : CompanyEquipmentSavedData.get(server).count(company.companyId(), machine);
        if (machineCapacity <= 0) return 0;
        if (recipe.workersPerCycle() <= 0) return Math.min(10000, machineCapacity);
        int workers = CompanyLaborSavedData.get(server).activeWorkers(company.companyId());
        return Math.min(10000, Math.min(machineCapacity, workers / recipe.workersPerCycle()));
    }

    private static boolean consumeInputsPreview(MinecraftServer server, Company company) {
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        var owner = com.ailudick.capitalismmod.market.InventoryOwner.company(company.companyId());
        for (Map.Entry<String, Integer> input : CompanyEconomy.inputs(company).entrySet()) {
            if (input.getValue() <= 0 || parseItem(input.getKey()) == null
                    || warehouse.count(owner, input.getKey()) < input.getValue()) return false;
        }
        return true;
    }

    public static boolean hire(Player player, String name, String role, int count, long dailyWage, int skill) {
        MinecraftServer server = player.getServer();
        Company company = getCompany(player, name);
        if (server == null || company == null || role == null || role.isBlank()
                || count <= 0 || count > 10000 || dailyWage <= 0 || dailyWage > Long.MAX_VALUE / count
                || skill < 0 || skill > 100) return false;
        CompanyLaborSavedData.get(server).add(new CompanyLaborSavedData.WorkerContract(
                UUID.randomUUID().toString(), company.companyId(), role.trim(), count, dailyWage, skill, true));
        return true;
    }

    public static boolean fire(Player player, String name, String contractId) {
        MinecraftServer server = player.getServer();
        Company company = getCompany(player, name);
        if (server == null || company == null || contractId == null) return false;
        boolean found = CompanyLaborSavedData.get(server).contracts(company.companyId()).stream()
                .anyMatch(contract -> contract.id().equals(contractId));
        if (!found) return false;
        CompanyLaborSavedData.get(server).remove(company.companyId(), contractId);
        return true;
    }

    public static boolean installMachine(Player player, String name, String machineId, int count) {
        MinecraftServer server = player.getServer();
        Company company = getCompany(player, name);
        MachineType type = MachineType.parse(machineId);
        if (server == null || company == null || type == null || type == MachineType.NONE || count <= 0
                || count > 10000 || type.purchasePrice() > Long.MAX_VALUE / count) return false;
        long cost = type.purchasePrice() * count;
        if (!debitTreasury(server, company.companyId(), Currencies.USD.id(), cost,
                "equipment_purchase", "购买生产设备 " + type.id() + " x" + count)) return false;
        CompanyEquipmentSavedData.get(server).install(company.companyId(), type, count);
        return true;
    }

    public static boolean maintainMachine(Player player, String name, String machineId) {
        MinecraftServer server = player.getServer();
        Company company = getCompany(player, name);
        MachineType type = MachineType.parse(machineId);
        if (server == null || company == null || type == null || type == MachineType.NONE) return false;
        CompanyEquipmentSavedData.Equipment equipment = CompanyEquipmentSavedData.get(server)
                .get(company.companyId(), type);
        if (equipment == null || equipment.count() <= 0 || equipment.condition() >= 100) return false;
        long missing = 100L - equipment.condition();
        long cost;
        try {
            cost = Math.max(1L, Math.multiplyExact(type.purchasePrice(), missing) / 100L);
        } catch (ArithmeticException e) {
            return false;
        }
        if (!debitTreasury(server, company.companyId(), Currencies.USD.id(), cost,
                "equipment_maintenance", "维护设备 " + type.id())) return false;
        return CompanyEquipmentSavedData.get(server).restore(company.companyId(), type, 100);
    }

    public static boolean selectRecipe(Player player, String name, String recipeId) {
        Company company = getCompany(player, name);
        IndustrySpec spec = company == null ? null : Industries.byId(company.type());
        if (company == null || spec == null || recipeId == null
                || spec.recipes().stream().noneMatch(recipe -> recipe.id().equals(recipeId))) return false;
        setCompany(player, name, company.withProductionRecipe(recipeId));
        return true;
    }

    /** Consumes the company's inputs from the warehouse, recording demand. Returns false if any input is short. */
    private static boolean consumeInputs(MinecraftServer server, Company company) {
        if (server == null) {
            return true;
        }
        Map<String, Integer> inputs = CompanyEconomy.inputs(company);
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        com.ailudick.capitalismmod.market.InventoryOwner owner =
                com.ailudick.capitalismmod.market.InventoryOwner.company(company.companyId());
        for (Map.Entry<String, Integer> input : inputs.entrySet()) {
            Item item = parseItem(input.getKey());
            if (item == null || input.getValue() <= 0
                    || warehouse.count(owner, input.getKey()) < input.getValue()) {
                return false;
            }
        }
        CommoditySavedData commodityData = CommoditySavedData.get(server);
        for (Map.Entry<String, Integer> input : inputs.entrySet()) {
            Item item = parseItem(input.getKey());
            warehouse.consume(owner, item, input.getValue());
            commodityData.addSupply(input.getKey(), -input.getValue());
        }
        return true;
    }

    /** Deposits the company's outputs into the warehouse, recording supply. */
    private static void produceOutputs(MinecraftServer server, Company company) {
        if (server == null) {
            return;
        }
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        com.ailudick.capitalismmod.market.InventoryOwner owner =
                com.ailudick.capitalismmod.market.InventoryOwner.company(company.companyId());
        CommoditySavedData commodityData = CommoditySavedData.get(server);
        for (Map.Entry<String, Integer> output : CompanyEconomy.outputs(company).entrySet()) {
            Item item = parseItem(output.getKey());
            if (item == null || output.getValue() <= 0) {
                continue;
            }
            warehouse.credit(owner, item, output.getValue());
            commodityData.ensureCommodity(output.getKey(),
                    Math.max(1L, CapitalismData.getCommodityPrices().getOrDefault(output.getKey(), 1L)));
            commodityData.addSupply(output.getKey(), output.getValue());
            // automatically fulfill any backorders for this commodity
            SupplyMarket.fulfill(server,
                    com.ailudick.capitalismmod.market.InventoryOwner.company(company.companyId()),
                    company.ownerUuid(), output.getKey());
        }
    }

    private static boolean canProduceOutputs(MinecraftServer server, Company company) {
        if (server == null) {
            return true;
        }
        for (Map.Entry<String, Integer> output : CompanyEconomy.outputs(company).entrySet()) {
            if (output.getValue() <= 0 || parseItem(output.getKey()) == null) {
                return false;
            }
        }
        return true;
    }

    private static long safeRate(long amount, double rate) {
        double value = amount * rate;
        if (!Double.isFinite(value) || value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, (long) value);
    }

    private static Item parseItem(String itemId) {
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            return (item == null || item == Items.AIR) ? null : item;
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    /** Lists a company on the stock exchange, issuing all shares to the founder. */
    public static boolean ipo(Player player, String name) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        Company company = getCompany(player, name);
        if (company == null) {
            return false;
        }
        EconomySavedData data = EconomySavedData.get(server);
        String stockId = stockId(player, name);
        if (data.isListed(stockId)) {
            return false;
        }
        long totalShares = Math.max(1000L, EconomyMath.multiply(company.registeredCapital(), 100L));
        if (totalShares < 0) {
            return false;
        }
        data.list(stockId, company.name(), company.registeredCapital(), totalShares);
        data.addShares(stockId, player.getUUID(), totalShares);
        return true;
    }

    /** Contributes founder money as paid-in capital and makes it available to the company. */
    public static boolean contributeCapital(Player player, String name, long amount) {
        if (amount <= 0) return false;
        Company company = getCompany(player, name);
        if (company == null || !EconomyHelper.tryPay(player, Currencies.USD, Money.toMinor(amount))) return false;
        long capital = EconomyMath.add(company.registeredCapital(), amount);
        if (capital < 0L || company.treasuryOf(Currencies.USD.id()) > Long.MAX_VALUE - amount) return false;
        Company funded = company.withRegisteredCapital(capital).addTreasury(Currencies.USD.id(), amount);
        if (funded == company) return false;
        setCompany(player, name, funded);
        MinecraftServer server = player.getServer();
        if (server != null) {
            CompanyLedgerSavedData.get(server).append(new CompanyLedgerEntry(
                    company.companyId(), server.overworld().getGameTime(), "capital_contribution",
                    Currencies.USD.id(), amount, funded.treasuryOf(Currencies.USD.id()), "paid-in capital"));
            EconomySavedData.get(server).updateListingCapital(stockId(player, name), capital);
        }
        return true;
    }

    /**
     * Distributes a declared dividend to the current shareholders of a listed
     * company. The company pays from cash first; dividends are not operating
     * expenses and each recipient receives a separate dividend-tax assessment.
     */
    public static boolean declareDividend(Player player, String name, long amountPerShare) {
        if (amountPerShare <= 0L || player.getServer() == null) return false;
        MinecraftServer server = player.getServer();
        Company company = getCompany(player, name);
        if (company == null) return false;
        EconomySavedData economy = EconomySavedData.get(server);
        String stockId = stockId(player, name);
        if (!economy.isListed(stockId)) return false;
        Map<String, Long> holders = economy.shareholders().get(stockId);
        if (holders == null || holders.isEmpty()) return false;

        Map<UUID, Long> payouts = new HashMap<>();
        long total = 0L;
        for (Map.Entry<String, Long> entry : holders.entrySet()) {
            UUID holder;
            try {
                holder = UUID.fromString(entry.getKey());
            } catch (IllegalArgumentException e) {
                return false;
            }
            long shares = entry.getValue() == null ? 0L : entry.getValue();
            long payout = EconomyMath.multiply(amountPerShare, shares);
            if (shares <= 0L || payout <= 0L) return false;
            total = EconomyMath.add(total, payout);
            if (total < 0L) return false;
            payouts.put(holder, payout);
        }
        if (company.treasuryOf(Currencies.USD.id()) < total || Money.toMinor(total) <= 0L) return false;

        Map<String, Long> treasury = new HashMap<>(company.treasury());
        long remaining = company.treasuryOf(Currencies.USD.id()) - total;
        treasury.put(Currencies.USD.id(), remaining);
        CompanySavedData.get(server).put(company.withTreasury(treasury));
        long now = server.overworld().getGameTime();
        CompanyLedgerSavedData.get(server).append(new CompanyLedgerEntry(
                company.companyId(), now, "dividend_distribution", Currencies.USD.id(),
                -total, remaining, "Dividend declared at USD " + amountPerShare + " per share"));

        String declaration = company.companyId() + ":dividend:" + UUID.randomUUID();
        for (Map.Entry<UUID, Long> payout : payouts.entrySet()) {
            long minor = Money.toMinor(payout.getValue());
            ServerPlayer online = server.getPlayerList().getPlayer(payout.getKey());
            if (online != null) {
                EconomyHelper.giveMoney(online, Currencies.USD, minor);
            } else {
                MarketMailboxSavedData.get(server).creditMoney(payout.getKey(), Currencies.USD.id(), minor);
            }
            TaxTransactionService.assess(server, TaxType.DIVIDEND, payout.getKey(),
                    Currencies.USD.id(), minor, declaration + ":" + payout.getKey(), now);
        }
        return true;
    }

    /** Withdraws {@code amount} of {@code currencyId} from a company's treasury to the founder. */
    public static boolean withdraw(Player player, String name, String currencyId, long amount) {
        if (!Currencies.exists(currencyId) || amount <= 0) {
            return false;
        }
        Company company = getCompany(player, name);
        if (company == null || company.treasuryOf(currencyId) < amount) {
            return false;
        }
        long amountMinor = Money.toMinor(amount);
        if (amountMinor <= 0) {
            return false;
        }
        EconomyHelper.giveMoney(player, Currencies.byId(currencyId), amountMinor);

        Map<String, Long> treasury = new HashMap<>(company.treasury());
        treasury.put(currencyId, company.treasuryOf(currencyId) - amount);
        setCompany(player, name, company.withTreasury(treasury));
        CompanyLedgerSavedData.get(player.getServer()).append(new CompanyLedgerEntry(
                company.companyId(), player.getServer().overworld().getGameTime(), "owner_withdrawal",
                currencyId, -amount, company.treasuryOf(currencyId) - amount, "业主提款"));
        return true;
    }

    /** Withdraws the entire USD treasury of a company to the founder. */
    public static boolean withdrawAll(Player player, String name) {
        Company company = getCompany(player, name);
        if (company == null || company.treasuryOf("usd") <= 0) {
            return false;
        }
        return withdraw(player, name, "usd", company.treasuryOf("usd"));
    }

    /** Pays a company's accrued corporate income tax through the unified tax ledger. */
    public static boolean payTax(Player player, String name) {
        Company company = getCompany(player, name);
        if (company == null || company.taxOwed() <= 0 || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        TaxSubject subject = new TaxSubject(TaxType.CORPORATE_INCOME, company.companyId(), company.ownerUuid());
        long legacyAmount = Money.toMinor(company.taxOwed());
        TaxService.ensureOutstanding(serverPlayer.getServer(), subject, "usd", legacyAmount,
                serverPlayer.level().getGameTime(), 0L, 0L);
        long outstanding = TaxService.outstanding(serverPlayer.getServer(), subject);
        if (outstanding <= 0L || !TaxService.pay(serverPlayer, subject, outstanding)) {
            return false;
        }
        setCompany(player, name, company.withTaxOwed(0));
        return true;
    }

    /** Updates the legacy company tax mirror after a unified tax payment. */
    public static void syncTaxMirror(Player player, String companyId, long outstandingMinor) {
        for (Map.Entry<String, Company> entry : getCompanies(player).entrySet()) {
            if (entry.getValue().companyId().equals(companyId)) {
                setCompany(player, entry.getKey(), entry.getValue().withTaxOwed(Money.toMajorCeiling(outstandingMinor)));
                return;
            }
        }
    }

    /** Transfers an unlisted company from seller to buyer after the buyer pays the agreed price. */
    public static boolean acquire(ServerPlayer seller, ServerPlayer buyer, AcquisitionSavedData.Offer offer) {
        if (offer == null || !offer.sellerUuid().equals(seller.getUUID())
                || !offer.buyerUuid().equals(buyer.getUUID()) || seller.getUUID().equals(buyer.getUUID())
                || offer.price() <= 0) {
            return false;
        }
        Company company = getCompany(seller, offer.companyName());
        if (company == null || getCompany(buyer, offer.companyName()) != null
                || isListed(seller, offer.companyName())) {
            return false;
        }
        if (!EconomyHelper.tryPay(buyer, Currencies.USD, Money.toMinor(offer.price()))) {
            return false;
        }
        removeCompany(seller, company.name());
        putCompany(buyer, company.name(), company);
        EconomyHelper.giveMoney(seller, Currencies.USD, Money.toMinor(offer.price()));
        TaxTransactionService.assess(buyer.getServer(), TaxType.CAPITAL_GAINS, seller.getUUID(), Currencies.USD.id(),
                Money.toMinorSaturated(offer.price()), "company-acquisition:" + offer.id(),
                buyer.getServer().overworld().getGameTime());
        return true;
    }

    /** Merges two unlisted companies owned by one player; both must use the same industry. */
    public static boolean merge(Player player, String sourceName, String targetName) {
        if (sourceName.equals(targetName)) {
            return false;
        }
        Company source = getCompany(player, sourceName);
        Company target = getCompany(player, targetName);
        if (source == null || target == null || !source.type().equals(target.type())) {
            return false;
        }
        if (isListed(player, sourceName) || isListed(player, targetName)) {
            return false;
        }
        if (source.taxOwed() > 0L || target.taxOwed() > 0L) {
            return false;
        }
        MinecraftServer server = player.getServer();
        if (server != null) {
            TaxSubject sourceTax = new TaxSubject(TaxType.CORPORATE_INCOME, source.companyId(), source.ownerUuid());
            TaxSubject targetTax = new TaxSubject(TaxType.CORPORATE_INCOME, target.companyId(), target.ownerUuid());
            if (TaxService.outstanding(server, sourceTax) > 0L || TaxService.outstanding(server, targetTax) > 0L
                    || CorporateTaxPeriodSavedData.get(server).hasPendingForCompany(source.companyId())
                    || CorporateTaxPeriodSavedData.get(server).hasPendingForCompany(target.companyId())
                    || CorporateTaxAnnualSavedData.get(server).hasPendingForCompany(source.companyId())
                    || CorporateTaxAnnualSavedData.get(server).hasPendingForCompany(target.companyId())) {
                return false;
            }
        }
        long capital = EconomyMath.add(source.registeredCapital(), target.registeredCapital());
        if (capital < 0L) return false;
        if (server != null) {
            WarehouseSavedData.get(server).transferAll(
                    com.ailudick.capitalismmod.market.InventoryOwner.company(source.companyId()),
                    com.ailudick.capitalismmod.market.InventoryOwner.company(target.companyId()));
            CompanyEquipmentSavedData.get(server).transferCompany(source.companyId(), target.companyId());
            CompanyLaborSavedData.get(server).transferCompany(source.companyId(), target.companyId());
            CompanyProductionSavedData.get(server).mergeCompany(source.companyId(), target.companyId());
            CompanyLoanSavedData.get(server).transferCompany(source.companyId(), target.companyId());
        }
        Map<String, Long> treasury = new HashMap<>(target.treasury());
        for (Map.Entry<String, Long> entry : source.treasury().entrySet()) {
            treasury.merge(entry.getKey(), entry.getValue(), (a, b) -> EconomyMath.add(a, b));
        }
        Company merged = new Company(target.companyId(), player.getUUID(), target.name(), target.type(), capital, treasury,
                EconomyMath.add(target.taxOwed(), source.taxOwed()), target.productionRecipe());
        removeCompany(player, sourceName);
        putCompany(player, targetName, merged);
        return true;
    }

    /** Returns the sole shareholder with more than half of a listed company's shares, if any. */
    public static UUID controller(MinecraftServer server, String stockId) {
        EconomySavedData data = EconomySavedData.get(server);
        EconomySavedData.Listing listing = data.listings().get(stockId);
        if (listing == null || listing.totalShares() <= 0) return null;
        for (Map.Entry<String, Long> entry : data.shareholders().getOrDefault(stockId, Map.of()).entrySet()) {
            if (entry.getValue() > listing.totalShares() / 2) {
                try {
                    return UUID.fromString(entry.getKey());
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /** Settles a public takeover offer by transferring a specific shareholder's shares. */
    public static boolean acceptPublicOffer(ServerPlayer seller, ServerPlayer buyer,
                                            PublicTakeoverSavedData.Offer offer) {
        if (offer == null || !offer.sellerUuid().equals(seller.getUUID())
                || !offer.buyerUuid().equals(buyer.getUUID()) || seller.getUUID().equals(buyer.getUUID())) {
            return false;
        }
        if (offer.pricePerShare() <= 0 || offer.quantity() <= 0) {
            return false;
        }
        EconomySavedData data = EconomySavedData.get(buyer.getServer());
        if (!data.isListed(offer.stockId()) || data.holdings(offer.stockId(), seller.getUUID()) < offer.quantity()) {
            return false;
        }
        long total = EconomyMath.multiply(offer.pricePerShare(), offer.quantity());
        if (total < 0 || !EconomyHelper.tryPay(buyer, Currencies.USD, Money.toMinor(total))) {
            return false;
        }
        data.addShares(offer.stockId(), seller.getUUID(), -offer.quantity());
        data.addShares(offer.stockId(), buyer.getUUID(), offer.quantity());
        EconomyHelper.giveMoney(seller, Currencies.USD, Money.toMinor(total));
        TaxTransactionService.assess(buyer.getServer(), TaxType.CAPITAL_GAINS, seller.getUUID(), Currencies.USD.id(),
                Money.toMinorSaturated(total), "public-takeover:" + offer.id(),
                buyer.getServer().overworld().getGameTime());
        return true;
    }

    /** Distributes {@code income} to the shareholders of a listed company, proportional to holdings. */
    private static void distributeDividend(MinecraftServer server, String stockId, long income) {
        EconomySavedData data = EconomySavedData.get(server);
        EconomySavedData.Listing listing = data.listings().get(stockId);
        Map<String, Long> holders = data.shareholders().get(stockId);
        if (listing == null || listing.totalShares() <= 0 || holders == null || holders.isEmpty()) {
            return;
        }
        long totalShares = listing.totalShares();
        for (Map.Entry<String, Long> entry : holders.entrySet()) {
            long portion = EconomyMath.multiply(income, entry.getValue());
            if (portion < 0) {
                continue;
            }
            portion = portion / totalShares;
            if (portion <= 0) {
                continue;
            }
            UUID holderId = UUID.fromString(entry.getKey());
            ServerPlayer holder = server.getPlayerList().getPlayer(holderId);
            if (holder != null) {
                EconomyHelper.giveMoney(holder, Currencies.USD, Money.toMinor(portion));
            } else {
                MarketMailboxSavedData.get(server).creditMoney(holderId, "usd", Money.toMinor(portion));
            }
        }
    }

    private static void setCompany(Player player, String name, Company company) {
        if (player.getServer() != null) {
            CompanySavedData.get(player.getServer()).put(company);
        }
    }

    private static void putCompany(Player player, String name, Company company) {
        company = company.withIdentity(company.companyId(), player.getUUID());
        Conglomerate conglomerate = getConglomerate(player);
        Map<String, String> companies = new HashMap<>(conglomerate.companies());
        companies.put(name, company.companyId());
        player.setData(ModAttachments.CONGLOMERATE, new Conglomerate(conglomerate.name(), companies));
        if (player.getServer() != null) {
            CompanySavedData.get(player.getServer()).put(company);
        }
    }

    private static void removeCompany(Player player, String name) {
        if (player.getServer() != null) {
            SupplyMarket.removeOffersForCompany(player.getServer(), player.getUUID(), name);
        }
        Conglomerate conglomerate = getConglomerate(player);
        Map<String, String> companies = new HashMap<>(conglomerate.companies());
        String removed = companies.remove(name);
        player.setData(ModAttachments.CONGLOMERATE, new Conglomerate(conglomerate.name(), companies));
        if (player.getServer() != null && removed != null) {
            CompanySavedData.get(player.getServer()).remove(removed);
        }
    }
}
