package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.economy.EconomySavedData;
import com.ailudick.capitalismmod.init.ModAttachments;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.supply.SupplyMarket;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import com.ailudick.capitalismmod.tax.TaxableIncomeEvent;
import com.ailudick.capitalismmod.tax.CorporateTaxPeriodSavedData;
import com.ailudick.capitalismmod.tax.CorporateTaxAnnualSavedData;
import com.ailudick.capitalismmod.tax.TaxTransactionService;
import com.ailudick.capitalismmod.economy.EconomyLogSavedData;
import com.ailudick.capitalismmod.economy.FinancialSettlementJournalSavedData;
import com.ailudick.capitalismmod.tax.TaxExpenseService;
import com.ailudick.capitalismmod.loan.CompanyLoanSavedData;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.data.CapitalismData;
import com.ailudick.capitalismmod.economy.labor.LaborMarketSavedData;
import com.ailudick.capitalismmod.economy.labor.LaborProfile;
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
            if (company != null && player.getUUID().equals(company.ownerUuid())) {
                result.put(entry.getKey(), company);
            }
        }
        return result;
    }

    /** Reconciles the player attachment from the authoritative world company registry after login. */
    public static int syncRegistryOwnership(ServerPlayer player) {
        if (player == null || player.getServer() == null) return 0;
        Conglomerate current = getConglomerate(player);
        Map<String, String> repaired = new HashMap<>();
        for (Map.Entry<String, String> entry : current.companies().entrySet()) {
            Company company = CompanySavedData.get(player.getServer()).get(entry.getValue());
            if (company != null && player.getUUID().equals(company.ownerUuid())) {
                repaired.put(entry.getKey(), entry.getValue());
            }
        }
        int added = 0;
        for (Company company : CompanySavedData.get(player.getServer()).companies().values()) {
            if (player.getUUID().equals(company.ownerUuid()) && !repaired.containsValue(company.companyId())) {
                repaired.put(company.name(), company.companyId());
                added++;
            }
        }
        if (!repaired.equals(current.companies())) {
            player.setData(ModAttachments.CONGLOMERATE, new Conglomerate(current.name(), repaired));
        }
        return added;
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

    /** Credits operating revenue once for a durable business source. */
    public static boolean creditTreasuryOnce(MinecraftServer server, String companyId, String currencyId,
                                              long amount, String sourceId) {
        if (server == null || companyId == null || currencyId == null || amount <= 0L
                || sourceId == null || sourceId.isBlank()) return false;
        String marker = "[source=" + sourceId + "]";
        if (CompanyLedgerSavedData.get(server).entries(companyId).stream()
                .anyMatch(entry -> entry.description() != null && entry.description().contains(marker))) {
            return true;
        }
        CompanySavedData data = CompanySavedData.get(server);
        Company company = data.get(companyId);
        if (company == null) return false;
        Company updated = company.addTreasury(currencyId, amount);
        if (updated == company) return false;
        data.put(updated);
        CompanyLedgerSavedData.get(server).append(new CompanyLedgerEntry(
                company.companyId(), server.overworld().getGameTime(), "revenue", currencyId,
                amount, updated.treasuryOf(currencyId), "Supply market sales [source=" + sourceId + "]"));
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

    /** Credits a non-operating inflow once for a durable business source. */
    public static boolean creditTreasuryNonOperatingOnce(MinecraftServer server, String companyId,
                                                          String currencyId, long amount,
                                                          String type, String description,
                                                          String sourceId) {
        if (sourceId == null || sourceId.isBlank()) return false;
        String marker = "[source=" + sourceId + "]";
        if (CompanyLedgerSavedData.get(server).entries(companyId).stream()
                .anyMatch(entry -> entry.description() != null && entry.description().contains(marker))) {
            return true;
        }
        String markedDescription = (description == null ? "" : description) + " " + marker;
        return creditTreasuryNonOperating(server, companyId, currencyId, amount, type, markedDescription);
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

    /** Idempotent non-operating debit keyed by a durable settlement source. */
    public static boolean debitTreasuryNonOperatingOnce(MinecraftServer server, String companyId, String currencyId,
                                                         long amount, String type, String description, String sourceId) {
        if (sourceId == null || sourceId.isBlank() || amount <= 0L) return false;
        String marker = "[source=" + sourceId + "]";
        if (CompanyLedgerSavedData.get(server).entries(companyId).stream()
                .anyMatch(entry -> entry.amount() < 0L && entry.description() != null && entry.description().contains(marker))) return true;
        return debitTreasuryNonOperating(server, companyId, currencyId, amount, type,
                (description == null ? "" : description) + " " + marker);
    }

    public static boolean hasNonOperatingDebitSource(MinecraftServer server, String companyId, String sourceId) {
        if (server == null || companyId == null || companyId.isBlank() || sourceId == null || sourceId.isBlank()) return false;
        String marker = "[source=" + sourceId + "]";
        return CompanyLedgerSavedData.get(server).entries(companyId).stream()
                .anyMatch(entry -> entry.amount() < 0L && entry.description() != null
                        && entry.description().contains(marker));
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
                recordTaxableExpense(server, company, type + ":" + server.overworld().getGameTime()
                                + ":" + UUID.randomUUID(),
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

    /** Records an expense that creates or increases a liability without moving cash. */
    public static boolean recordNonCashExpense(MinecraftServer server, Company company, String sourceId,
                                                long expense, String currencyId, String description) {
        if (server == null || company == null || sourceId == null || sourceId.isBlank()
                || expense <= 0L || currencyId == null || currencyId.isBlank()) return false;
        long occurredAt = server.overworld().getGameTime();
        if (!recordTaxableExpense(server, company, sourceId, expense, currencyId, occurredAt)) return false;
        CompanyLedgerSavedData.get(server).append(new CompanyLedgerEntry(
                company.companyId(), occurredAt, "accrued_expense", currencyId, -expense,
                company.treasuryOf(currencyId), description == null ? "Accrued expense" : description));
        return true;
    }

    /**
     * Removes the cost basis of goods sold from the company's inventory ledger.
     *
     * <p>This is a non-cash accounting event: the goods have already left the
     * warehouse, so the treasury must not be charged again. Tracked weighted
     * average cost layers are consumed first; legacy stock without a layer uses
     * the current commodity price as a conservative fallback.</n+     */
    public static long recordInventorySale(MinecraftServer server, String companyId,
                                           String itemId, int quantity, String sourceId) {
        if (server == null || companyId == null || companyId.isBlank()
                || itemId == null || itemId.isBlank() || quantity <= 0
                || sourceId == null || sourceId.isBlank() || parseItem(itemId) == null) {
            return 0L;
        }
        Company company = CompanySavedData.get(server).get(companyId);
        if (company == null) return 0L;

        CompanyInventoryCostSavedData inventoryCosts = CompanyInventoryCostSavedData.get(server);
        if (inventoryCosts.hasInventorySale(sourceId)) return 0L;
        String accountingSource = "inventory_cogs:" + sourceId;
        String ledgerMarker = "[source=" + accountingSource + "]";
        if (CompanyLedgerSavedData.get(server).entries(companyId).stream()
                .anyMatch(entry -> entry.description() != null && entry.description().contains(ledgerMarker))) {
            return 0L;
        }
        CompanyInventoryCostSavedData.Consumption tracked =
                inventoryCosts.consume(companyId, itemId, quantity);
        CompanyQualitySavedData.get(server).consume(companyId, itemId, quantity);
        int untracked = quantity - tracked.quantity();
        long fallback = untracked <= 0 ? 0L
                : EconomyMath.multiply(Math.max(0L, CommoditySavedData.get(server).price(itemId)), untracked);
        long cost = EconomyMath.add(tracked.cost(), fallback);
        if (cost <= 0L) {
            inventoryCosts.recordInventorySale(sourceId);
            return 0L;
        }

        long occurredAt = server.overworld().getGameTime();
        recordTaxableExpense(server, company, accountingSource,
                cost, Currencies.USD.id(), occurredAt);
        // Cost of goods sold is non-cash: the inventory asset is exchanged for
        // revenue, so keep the cash-flow ledger balance unchanged while still
        // exposing the expense to profit reporting.
        CompanyLedgerSavedData.get(server).append(new CompanyLedgerEntry(
                company.companyId(), occurredAt, "cost_of_goods_sold", Currencies.USD.id(),
                -cost, company.treasuryOf(Currencies.USD.id()),
                "Inventory cost of goods sold: " + itemId + " x" + quantity
                        + " [source=" + accountingSource + "]"));
        inventoryCosts.recordInventorySale(sourceId);
        return cost;
    }

    /** Records a non-cash inventory impairment without charging the treasury again. */
    public static boolean recordInventoryLoss(MinecraftServer server, String companyId, long amount, String shipmentId) {
        if (server == null || companyId == null || companyId.isBlank() || amount <= 0L
                || shipmentId == null || shipmentId.isBlank()) return false;
        Company company = CompanySavedData.get(server).get(companyId);
        if (company == null) return false;
        CompanyInventoryCostSavedData inventoryCosts = CompanyInventoryCostSavedData.get(server);
        if (!inventoryCosts.recordInventoryLoss(shipmentId)) return false;
        CompanyLedgerSavedData.get(server).append(new CompanyLedgerEntry(company.companyId(),
                server.overworld().getGameTime(), "inventory_loss", Currencies.USD.id(), -amount,
                company.treasuryOf(Currencies.USD.id()), "运输损失：shipment " + shipmentId));
        return true;
    }

    /** Disposes the held output of a rejected quality batch and recognizes its inventory loss. */
    public static long disposeQualityBatch(MinecraftServer server, Company company, String batchId) {
        if (server == null || company == null || batchId == null || batchId.isBlank()) return 0L;
        CompanyQualityHoldSavedData holds = CompanyQualityHoldSavedData.get(server);
        var held = holds.forBatch(company.companyId(), batchId);
        if (held.isEmpty()) return 0L;
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        InventoryOwner owner = InventoryOwner.company(company.companyId());
        CompanyInventoryCostSavedData costs = CompanyInventoryCostSavedData.get(server);
        long loss = 0L;
        for (CompanyQualityHoldSavedData.Hold hold : held) {
            Item item = parseItem(hold.itemId());
            if (item == null) continue;
            int quantity = Math.min(hold.quantity(), warehouse.count(owner, hold.itemId()));
            if (quantity <= 0 || !warehouse.consume(owner, item, quantity)) continue;
            CompanyInventoryCostSavedData.Consumption tracked = costs.consume(
                    company.companyId(), hold.itemId(), quantity);
            int untracked = quantity - tracked.quantity();
            long fallback = untracked <= 0 ? 0L
                    : EconomyMath.multiply(Math.max(0L, CommoditySavedData.get(server).price(hold.itemId())), untracked);
            loss = EconomyMath.add(loss, EconomyMath.add(tracked.cost(), fallback));
            CompanyQualitySavedData.get(server).consume(company.companyId(), hold.itemId(), quantity);
        }
        holds.removeBatch(company.companyId(), batchId);
        if (loss > 0L) {
            long now = server.overworld().getGameTime();
            recordTaxableExpense(server, company, "quality_rejection:" + batchId,
                    loss, Currencies.USD.id(), now);
            CompanyLedgerSavedData.get(server).append(new CompanyLedgerEntry(
                    company.companyId(), now, "quality_rejection_loss", Currencies.USD.id(),
                    -loss, company.treasuryOf(Currencies.USD.id()),
                    "Disposed rejected production batch " + batchId));
        }
        return loss;
    }

    /** Releases a held batch into the market only after an explicit quality approval. */
    public static int releaseQualityBatch(MinecraftServer server, Company company, String batchId) {
        if (server == null || company == null || batchId == null || batchId.isBlank()) return 0;
        CompanyQualityHoldSavedData holds = CompanyQualityHoldSavedData.get(server);
        var held = holds.forBatch(company.companyId(), batchId);
        if (held.isEmpty()) return 0;
        if (!holds.releaseBatch(company.companyId(), batchId)) return 0;
        CommoditySavedData commodities = CommoditySavedData.get(server);
        int released = 0;
        for (CompanyQualityHoldSavedData.Hold hold : held) {
            if (hold.quantity() <= 0 || parseItem(hold.itemId()) == null) continue;
            commodities.ensureCommodity(hold.itemId(),
                    Math.max(1L, CapitalismData.getCommodityPrices().getOrDefault(hold.itemId(), 1L)));
            commodities.addSupply(hold.itemId(), hold.quantity());
            SupplyMarket.fulfill(server, InventoryOwner.company(company.companyId()),
                    company.ownerUuid(), hold.itemId());
            released = Math.min(Integer.MAX_VALUE, released + hold.quantity());
        }
        return released;
    }

    /** Charges a documented rework cost and sends a held batch back to inspection. */
    public static int reworkQualityBatch(MinecraftServer server, Company company, String batchId) {
        if (server == null || company == null || batchId == null || batchId.isBlank()) return -1;
        CompanyProductionBatchSavedData.Batch batch = CompanyProductionBatchSavedData.get(server)
                .find(company.companyId(), batchId);
        CompanyQualityControlSavedData.Inspection inspection = CompanyQualityControlSavedData.get(server)
                .find(company.companyId(), batchId);
        if (batch == null || inspection == null || !"rework".equalsIgnoreCase(inspection.status())) return -1;
        long reworkCost = Math.max(1L, (long) Math.ceil(batch.conversionCost()
                * Config.COMPANY_QUALITY_REWORK_COST_RATE.get()));
        if (!debitTreasury(server, company.companyId(), Currencies.USD.id(), reworkCost,
                "quality_rework", "Quality rework for batch " + batchId)) return -1;
        int improvedScore = Math.min(100, batch.qualityScore()
                + Config.COMPANY_QUALITY_REWORK_SCORE_GAIN.get());
        if (!CompanyQualityControlSavedData.get(server).reinspect(company.companyId(), batchId,
                improvedScore, server.overworld().getGameTime())) return -1;
        int scoreDelta = Math.max(0, improvedScore - batch.qualityScore());
        if (scoreDelta > 0) {
            for (Map.Entry<String, Integer> output : batch.outputs().entrySet()) {
                CompanyQualitySavedData.get(server).improve(company.companyId(), output.getKey(),
                        Math.max(0, output.getValue()), scoreDelta);
            }
        }
        return improvedScore;
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
            net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(player.blockPosition());
            String dimension = player.level().dimension().location().toString();
            if (com.ailudick.capitalismmod.land.LandHelper.hasCommercialRight(player.getServer(), dimension,
                    chunk.x, chunk.z, company.ownerUuid())) {
                CompanySiteSavedData.get(player.getServer()).set(new CompanySiteSavedData.Site(company.companyId(),
                        dimension, chunk.x, chunk.z));
                OilFieldSavedData.get(player.getServer()).prospect(dimension, chunk.x, chunk.z);
            }
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

    /** Result of one production attempt, including a stable reporting reason. */
    public record ProductionCycleResult(boolean success, String failureReason) {
        public static ProductionCycleResult completed() {
            return new ProductionCycleResult(true, "");
        }

        public static ProductionCycleResult failure(String reason) {
            return new ProductionCycleResult(false, reason == null || reason.isBlank()
                    ? "unknown" : reason);
        }
    }

    /** Runs one atomic recipe batch: all inputs are checked before any are consumed. */
    public static boolean runProductionCycle(MinecraftServer server, Company company) {
        return runProductionCycleResult(server, company).success();
    }

    /** Runs one batch and reports why a non-mutating precondition failed. */
    public static ProductionCycleResult runProductionCycleResult(MinecraftServer server, Company company) {
        return runProductionCycleResult(server, company, null);
    }

    /** Runs a batch with an optional stable cycle key for audit and tax linkage. */
    public static ProductionCycleResult runProductionCycleResult(MinecraftServer server, Company company,
                                                                  String cycleKey) {
        if (server == null || company == null || company.registeredCapital() <= 0) {
            return ProductionCycleResult.failure("invalid_company");
        }
        String stableBatchId = ProductionCycleIdentity.batchId(cycleKey);
        if (!stableBatchId.isBlank()
                && CompanyProductionBatchSavedData.get(server).findById(stableBatchId) != null) {
            return ProductionCycleResult.completed();
        }
        if (!CompanyLifecycleService.canOperate(server, company.companyId())) return ProductionCycleResult.failure("company_inactive");
        // Unpaid wages are a persistent labor liability. Employees do not
        // continue producing new batches while the liability is outstanding.
        if (CompanyPayrollSavedData.get(server).unpaid(company.companyId()) > 0L) return ProductionCycleResult.failure("unpaid_wages");
        ProductionRecipe recipe = CompanyEconomy.recipe(company);
        if (recipe == null) return ProductionCycleResult.failure("missing_recipe");
        boolean serviceCycle = recipe.isService();
        if (!serviceCycle && recipe.outputs().isEmpty()) return ProductionCycleResult.failure("no_outputs");
        if (!canProduceOutputs(server, company)) return ProductionCycleResult.failure("output_capacity");
        if (!canConsumeInputs(server, company)) return ProductionCycleResult.failure("missing_inputs");
        if (serviceCycle) {
            Company current = CompanySavedData.get(server).get(company.companyId());
            if (current == null || EconomyMath.add(current.treasuryOf(Currencies.USD.id()), recipe.income()) < 0L) {
                return ProductionCycleResult.failure("service_settlement");
            }
        }
        MachineType machine = MachineType.parse(recipe.machineType());
        if (machine == null) return ProductionCycleResult.failure("invalid_machine");
        CompanySiteAllocationSavedData allocations = CompanySiteAllocationSavedData.get(server);
        CompanySiteSavedData.Site operatingSite = null;
        OilFieldSavedData.Field oilField = null;
        if (machine == MachineType.OIL_WELL) {
            operatingSite = CompanySiteSavedData.get(server).sites(company.companyId()).stream()
                    .filter(candidate -> com.ailudick.capitalismmod.land.LandHelper.hasCommercialRight(server,
                            candidate.dimension(), candidate.chunkX(), candidate.chunkZ(), company.ownerUuid()))
                    .filter(candidate -> siteCanRun(allocations, company, candidate, machine))
                    .map(candidate -> new Object[]{candidate, OilFieldSavedData.get(server).get(candidate.dimension(),
                            candidate.chunkX(), candidate.chunkZ())})
                    .filter(pair -> pair[1] instanceof OilFieldSavedData.Field field
                            && OilFieldSavedData.get(server).canExtract(field, 3L))
                    .map(pair -> (CompanySiteSavedData.Site) pair[0])
                    .findFirst().orElse(null);
            if (operatingSite == null) return ProductionCycleResult.failure("missing_oil_site");
            oilField = OilFieldSavedData.get(server).get(operatingSite.dimension(), operatingSite.chunkX(), operatingSite.chunkZ());
            if (oilField == null) return ProductionCycleResult.failure("missing_oil_site");
        } else {
            // A batch can still be produced by legacy companies without a
            // registered site, but new batches record the first authorized
            // operating site when one exists.
            operatingSite = CompanySiteSavedData.get(server).sites(company.companyId()).stream()
                    .filter(candidate -> com.ailudick.capitalismmod.land.LandHelper.hasCommercialRight(server,
                            candidate.dimension(), candidate.chunkX(), candidate.chunkZ(), company.ownerUuid()))
                    .filter(candidate -> siteCanRun(allocations, company, candidate, machine))
                    .findFirst().orElse(null);
        }
        CompanyLaborSavedData labor = CompanyLaborSavedData.get(server);
        int availableWorkers = labor.activeWorkers(company.companyId());
        int assignedWorkers = allocations.workerCount(company.companyId(), operatingSite);
        if (assignedWorkers >= 0) availableWorkers = assignedWorkers;
        if (recipe.workersPerCycle() > 0 && availableWorkers < recipe.workersPerCycle()) {
            return ProductionCycleResult.failure("insufficient_workers");
        }
        int availableMachines = machine == MachineType.NONE ? 1
                : CompanyEquipmentSavedData.get(server).count(company.companyId(), machine);
        if (machine != MachineType.NONE) {
            int assignedMachines = allocations.machineCount(company.companyId(), operatingSite, machine.id());
            if (assignedMachines >= 0) availableMachines = Math.min(availableMachines, assignedMachines);
        }
        if (availableMachines <= 0) {
            return ProductionCycleResult.failure("missing_equipment");
        }
        long machineCost = machine == MachineType.NONE ? 0L : machine.maintenancePerCycle();
        long cost;
        try {
            long operatingOverhead = Config.COMPANY_FIXED_OVERHEAD_PER_CYCLE.get();
            cost = Math.addExact(Math.addExact(Math.addExact(0L, Math.max(0L, recipe.energyCost())),
                            Math.max(0L, recipe.maintenanceCost())),
                    Math.addExact(machineCost, operatingOverhead));
        } catch (ArithmeticException e) {
            return ProductionCycleResult.failure("cost_overflow");
        }
        if (!debitTreasury(server, company.companyId(),
                Currencies.USD.id(), cost, "production_expense", "生产周期劳动力、能源与设备维护成本")) {
            return ProductionCycleResult.failure("insufficient_funds");
        }
        InputConsumption inputConsumption = consumeInputs(server, company);
        if (!inputConsumption.success()) {
            return ProductionCycleResult.failure("input_reservation");
        }
        long occurredAt = server.overworld().getGameTime();
        String serviceSource = serviceCycle
                ? "service_cycle:" + company.companyId() + ":"
                + (cycleKey == null || cycleKey.isBlank() ? occurredAt + ":" + UUID.randomUUID() : cycleKey)
                : "";
        if (serviceCycle) {
            if (!creditTreasuryOnce(server, company.companyId(), Currencies.USD.id(), recipe.income(), serviceSource)) {
                return ProductionCycleResult.failure("service_income");
            }
            Company current = CompanySavedData.get(server).get(company.companyId());
            if (current != null) {
                recordTaxableIncome(server, current,
                        serviceSource, recipe.income(),
                        Currencies.USD.id(), occurredAt);
                // Services are taxable supplies too. The company owner is the
                // current tax subject until a separate legal-entity taxpayer
                // model exists; the source key keeps the VAT invoice idempotent.
                TaxTransactionService.assess(server, current.ownerUuid(), Currencies.USD.id(),
                        Money.toMinorSaturated(recipe.income()), "vat:" + serviceSource, occurredAt);
            }
            // Service production has no warehouse output, so preserve the
            // delivery economics separately for later contracts/invoicing.
            // Payroll remains a separate daily accrual and is intentionally
            // not duplicated in this cycle record.
        }
        long equipmentDepreciation = machine == MachineType.NONE ? 0L
                : CompanyEquipmentSavedData.get(server).useAndMeasureBookValueLoss(company.companyId(), machine);
        if (equipmentDepreciation < 0L) return ProductionCycleResult.failure("equipment_accounting");
        long conversionCost = EconomyMath.add(inputConsumption.cost(), cost);
        conversionCost = EconomyMath.add(Math.max(0L, conversionCost), equipmentDepreciation);
        if (conversionCost < 0L) conversionCost = Long.MAX_VALUE;
        if (oilField != null && !OilFieldSavedData.get(server).extract(oilField, 3L)) return ProductionCycleResult.failure("oil_reservation");
        int qualityScore = productionQuality(server, company, machine);
        produceOutputs(server, company, recipe, conversionCost, qualityScore, operatingSite, cycleKey, stableBatchId);
        if (serviceCycle) {
            CompanyServiceDeliverySavedData.get(server).append(
                    new CompanyServiceDeliverySavedData.ServiceDelivery(
                            company.companyId(), server.overworld().getGameTime(), recipe.id(),
                            recipe.income(), inputConsumption.cost(), cost,
                            Math.max(0L, equipmentDepreciation), recipe.workersPerCycle(), serviceSource));
        }
        if (equipmentDepreciation > 0L) {
            Company current = CompanySavedData.get(server).get(company.companyId());
            if (current != null) {
                recordNonCashExpense(server, current,
                        "depreciation:" + company.companyId() + ":" + UUID.randomUUID(),
                        equipmentDepreciation, Currencies.USD.id(),
                        "Production equipment depreciation");
            }
        }
        return ProductionCycleResult.completed();
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
        CompanySiteAllocationSavedData allocations = CompanySiteAllocationSavedData.get(server);
        if (machine != MachineType.NONE && allocations.hasMachineAllocation(company.companyId(), machine.id())) {
            machineCapacity = Math.min(machineCapacity,
                    allocations.maxMachineCount(company.companyId(), machine.id()));
        }
        if (machineCapacity <= 0) return 0;
        if (recipe.workersPerCycle() <= 0) return Math.min(10000, machineCapacity);
        LaborMarketSavedData laborMarket = LaborMarketSavedData.get(server);
        int effectiveMarketWorkers = laborMarket.activeForEmployer(company.companyId()).stream()
                .map(employment -> laborMarket.profile(employment.workerId()))
                .filter(java.util.Objects::nonNull)
                .mapToInt(LaborProfile::participation).sum();
        int marketWorkers = laborMarket.activeWorkers(company.companyId());
        if (marketWorkers > 0) marketWorkers = Math.max(1, effectiveMarketWorkers / 100);
        int workers = CompanyLaborSavedData.get(server).activeWorkers(company.companyId()) + marketWorkers;
        if (allocations.hasWorkerAllocation(company.companyId())) {
            workers = Math.min(workers, allocations.maxWorkerCount(company.companyId()));
        }
        return Math.min(10000, Math.min(machineCapacity, workers / recipe.workersPerCycle()));
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
        CompanySiteAllocationSavedData.get(server).trimWorkerCount(company.companyId(),
                CompanyLaborSavedData.get(server).activeWorkers(company.companyId()));
        return true;
    }

    public static boolean installMachine(Player player, String name, String machineId, int count) {
        MinecraftServer server = player.getServer();
        Company company = getCompany(player, name);
        MachineType type = MachineType.parse(machineId);
        if (server == null || company == null || type == null || type == MachineType.NONE || count <= 0
                || count > 10000 || type.purchasePrice() > Long.MAX_VALUE / count) return false;
        long cost = type.purchasePrice() * count;
        Map<String, Integer> materials = machineMaterials(type, count);
        com.ailudick.capitalismmod.market.InventoryOwner owner =
                com.ailudick.capitalismmod.market.InventoryOwner.company(company.companyId());
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        if (materials == null || !warehouse.canConsumeBatch(owner, materials)) return false;
        if (!debitTreasuryNonOperating(server, company.companyId(), Currencies.USD.id(), cost,
                "equipment_purchase", "购买生产设备 " + type.id() + " x" + count)) return false;
        if (!warehouse.consumeBatch(owner, materials)) {
            creditTreasuryNonOperating(server, company.companyId(), Currencies.USD.id(), cost,
                    "equipment_purchase_rollback", "Equipment material reservation failed");
            return false;
        }
        consumeInventoryCostLayers(server, company.companyId(), materials);
        CommoditySavedData commodityData = CommoditySavedData.get(server);
        for (Map.Entry<String, Integer> material : materials.entrySet()) {
            commodityData.addSupply(material.getKey(), -material.getValue());
        }
        CompanyEquipmentSavedData.get(server).install(company.companyId(), type, count);
        return true;
    }

    private static Map<String, Integer> machineMaterials(MachineType type, int count) {
        Map<String, Integer> materials = new HashMap<>();
        for (Map.Entry<String, Integer> material : type.materials().entrySet()) {
            int perMachine = material.getValue() == null ? 0 : material.getValue();
            if (perMachine <= 0) return null;
            try {
                materials.put(material.getKey(), Math.multiplyExact(perMachine, count));
            } catch (ArithmeticException e) {
                return null;
            }
        }
        return materials;
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
        Map<String, Integer> materials = machineMaintenanceMaterials(type, (int) missing);
        com.ailudick.capitalismmod.market.InventoryOwner owner =
                com.ailudick.capitalismmod.market.InventoryOwner.company(company.companyId());
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        if (materials == null || !warehouse.canConsumeBatch(owner, materials)) return false;
        if (!debitTreasury(server, company.companyId(), Currencies.USD.id(), cost,
                "equipment_maintenance", "维护设备 " + type.id())) return false;
        if (!warehouse.consumeBatch(owner, materials)) {
            creditTreasuryNonOperating(server, company.companyId(), Currencies.USD.id(), cost,
                    "equipment_maintenance_rollback", "Equipment maintenance material reservation failed");
            return false;
        }
        consumeInventoryCostLayers(server, company.companyId(), materials);
        CommoditySavedData commodityData = CommoditySavedData.get(server);
        for (Map.Entry<String, Integer> material : materials.entrySet()) {
            commodityData.addSupply(material.getKey(), -material.getValue());
        }
        return CompanyEquipmentSavedData.get(server).restore(company.companyId(), type, 100);
    }

    /** Sells installed equipment and records the disposal gain or loss against book value. */
    public static boolean sellMachine(Player player, String name, String machineId, int count, long salePrice) {
        MinecraftServer server = player.getServer();
        Company company = getCompany(player, name);
        MachineType type = MachineType.parse(machineId);
        if (server == null || company == null || type == null || type == MachineType.NONE
                || count <= 0 || salePrice < 0L) return false;
        CompanyEquipmentSavedData equipment = CompanyEquipmentSavedData.get(server);
        long bookValue = equipment.bookValue(company.companyId(), type, count);
        if (bookValue <= 0L || salePrice > Long.MAX_VALUE - company.treasuryOf(Currencies.USD.id()))
                return false;
        if (!equipment.remove(company.companyId(), type, count)) return false;
        if (salePrice > 0L && !creditTreasuryNonOperating(server, company.companyId(),
                Currencies.USD.id(), salePrice, "equipment_disposal",
                "Equipment disposal proceeds " + type.id() + " x" + count)) {
            // This should only be reachable on an arithmetic/storage failure; restore the
            // equipment rather than silently destroying an asset.
            equipment.install(company.companyId(), type, count);
            return false;
        }
        CompanyEquipmentSavedData.Equipment remainingEquipment = equipment.get(company.companyId(), type);
        CompanySiteAllocationSavedData.get(server).trimMachineCount(company.companyId(), type.id(),
                remainingEquipment == null ? 0 : remainingEquipment.count());
        long occurredAt = server.overworld().getGameTime();
        CompanyLedgerSavedData.get(server).append(new CompanyLedgerEntry(
                company.companyId(), occurredAt, "equipment_disposal_book_value", Currencies.USD.id(),
                -bookValue, company.treasuryOf(Currencies.USD.id()),
                "Remove carrying value for " + type.id() + " x" + count));
        if (salePrice > bookValue) {
            recordTaxableIncome(server, company, "equipment_disposal_gain:" + occurredAt,
                    salePrice - bookValue, Currencies.USD.id(), occurredAt);
        } else if (bookValue > salePrice) {
            recordTaxableExpense(server, company, "equipment_disposal_loss:" + occurredAt,
                    bookValue - salePrice, Currencies.USD.id(), occurredAt);
        }
        return true;
    }

    private static Map<String, Integer> machineMaintenanceMaterials(MachineType type, int conditionMissing) {
        Map<String, Integer> materials = new HashMap<>();
        for (Map.Entry<String, Integer> material : type.materials().entrySet()) {
            int perMachine = material.getValue() == null ? 0 : material.getValue();
            if (perMachine <= 0) return null;
            try {
                int product = Math.multiplyExact(perMachine, conditionMissing);
                materials.put(material.getKey(), product / 100 + (product % 100 == 0 ? 0 : 1));
            } catch (ArithmeticException e) {
                return null;
            }
        }
        return materials;
    }

    public static boolean selectRecipe(Player player, String name, String recipeId) {
        Company company = getCompany(player, name);
        IndustrySpec spec = company == null ? null : Industries.byId(company.type());
        if (company == null || spec == null || recipeId == null
                || spec.recipes().stream().noneMatch(recipe -> recipe.id().equals(recipeId))) return false;
        setCompany(player, name, company.withProductionRecipe(recipeId));
        return true;
    }

    private record InputConsumption(boolean success, long cost) {
    }

    /** Consumes the company's inputs from the warehouse and returns their cost. */
    private static InputConsumption consumeInputs(MinecraftServer server, Company company) {
        if (server == null) {
            return new InputConsumption(true, 0L);
        }
        Map<String, Integer> inputs = CompanyEconomy.inputs(company);
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        com.ailudick.capitalismmod.market.InventoryOwner owner =
                com.ailudick.capitalismmod.market.InventoryOwner.company(company.companyId());
        if (!warehouse.consumeBatch(owner, inputs)) return new InputConsumption(false, 0L);
        CommoditySavedData commodityData = CommoditySavedData.get(server);
        for (Map.Entry<String, Integer> input : inputs.entrySet()) {
            commodityData.addSupply(input.getKey(), -input.getValue());
        }
        long inventoryCost = inventoryConsumptionCost(server, company, inputs);
        if (inventoryCost > 0L) {
            long occurredAt = server.overworld().getGameTime();
            recordTaxableExpense(server, company,
                    "inventory_consumption:" + company.companyId() + ":" + occurredAt + ":" + UUID.randomUUID(),
                    inventoryCost, Currencies.USD.id(), occurredAt);
        }
        return new InputConsumption(true, inventoryCost);
    }

    private static void addProducedInventoryCosts(MinecraftServer server, String companyId,
                                                   Map<String, Integer> outputs, long totalCost) {
        if (server == null || companyId == null || outputs == null || outputs.isEmpty() || totalCost <= 0L) return;
        long totalQuantity = 0L;
        int validOutputs = 0;
        for (Map.Entry<String, Integer> output : outputs.entrySet()) {
            if (output.getKey() != null && output.getValue() != null && output.getValue() > 0
                    && parseItem(output.getKey()) != null) {
                totalQuantity = EconomyMath.add(totalQuantity, output.getValue());
                validOutputs++;
            }
        }
        if (totalQuantity <= 0L || validOutputs <= 0) return;
        long allocated = 0L;
        int index = 0;
        CompanyInventoryCostSavedData costs = CompanyInventoryCostSavedData.get(server);
        for (Map.Entry<String, Integer> output : outputs.entrySet()) {
            if (output.getKey() == null || output.getValue() == null || output.getValue() <= 0
                    || parseItem(output.getKey()) == null) continue;
            index++;
            long share = index == validOutputs ? Math.max(0L, totalCost - allocated)
                    : proportionalCost(totalCost, output.getValue(), totalQuantity);
            allocated = EconomyMath.add(allocated, share);
            costs.add(companyId, output.getKey(), output.getValue(), share);
        }
    }

    /** Estimates the cost of consumed inventory without changing warehouse state. */
    private static long inventoryConsumptionCost(MinecraftServer server, Company company,
                                                  Map<String, Integer> inputs) {
        if (server == null || inputs == null || inputs.isEmpty()) return 0L;
        CommoditySavedData market = CommoditySavedData.get(server);
        long total = 0L;
        for (Map.Entry<String, Integer> input : inputs.entrySet()) {
            if (input.getKey() == null || input.getValue() == null || input.getValue() <= 0) continue;
            CompanyInventoryCostSavedData.Consumption tracked =
                    CompanyInventoryCostSavedData.get(server).consume(company.companyId(), input.getKey(), input.getValue());
            int untracked = input.getValue() - tracked.quantity();
            long line = tracked.cost();
            if (untracked > 0) {
                long unitPrice = Math.max(0L, market.price(input.getKey()));
                line = EconomyMath.add(line, EconomyMath.multiply(unitPrice, untracked));
            }
            total = EconomyMath.add(total, line);
            if (total < 0L) return Long.MAX_VALUE;
        }
        return Math.max(0L, total);
    }

    private static long proportionalCost(long total, long quantity, long denominator) {
        if (total <= 0L || quantity <= 0L || denominator <= 0L) return 0L;
        if (total == Long.MAX_VALUE) return Long.MAX_VALUE;
        long whole = total / denominator;
        long remainder = total % denominator;
        long result = EconomyMath.multiply(whole, quantity);
        result = EconomyMath.add(result, EconomyMath.multiply(remainder, quantity) / denominator);
        return result < 0L ? Long.MAX_VALUE : result;
    }

    private static void consumeInventoryCostLayers(MinecraftServer server, String companyId,
                                                    Map<String, Integer> materials) {
        if (server == null || companyId == null || materials == null) return;
        CompanyInventoryCostSavedData costs = CompanyInventoryCostSavedData.get(server);
        for (Map.Entry<String, Integer> material : materials.entrySet()) {
            if (material.getKey() != null && material.getValue() != null && material.getValue() > 0) {
                costs.consume(companyId, material.getKey(), material.getValue());
            }
        }
    }

    private static boolean canConsumeInputs(MinecraftServer server, Company company) {
        if (server == null || company == null) return false;
        Map<String, Integer> inputs = CompanyEconomy.inputs(company);
        InventoryOwner owner = InventoryOwner.company(company.companyId());
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        CompanyQualityHoldSavedData holds = CompanyQualityHoldSavedData.get(server);
        for (Map.Entry<String, Integer> input : inputs.entrySet()) {
            int required = input.getValue() == null ? 0 : input.getValue();
            if (required <= 0 || parseItem(input.getKey()) == null
                    || holds.availableUnits(company.companyId(), input.getKey(), warehouse.count(owner, input.getKey())) < required) {
                return false;
            }
        }
        return true;
    }

    /** Deposits outputs, records their conversion cost, then fulfills backorders. */
    private static void produceOutputs(MinecraftServer server, Company company, ProductionRecipe recipe,
                                       long conversionCost, int qualityScore,
                                       CompanySiteSavedData.Site operatingSite, String cycleKey,
                                       String stableBatchId) {
        if (server == null) {
            return;
        }
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        com.ailudick.capitalismmod.market.InventoryOwner owner =
                com.ailudick.capitalismmod.market.InventoryOwner.company(company.companyId());
        CommoditySavedData commodityData = CommoditySavedData.get(server);
        Map<String, Integer> outputs = recipe.outputs();
        for (Map.Entry<String, Integer> output : outputs.entrySet()) {
            Item item = parseItem(output.getKey());
            if (item == null || output.getValue() <= 0) {
                continue;
            }
            warehouse.credit(owner, item, output.getValue());
            CompanyQualitySavedData.get(server).record(company.companyId(), output.getKey(),
                    output.getValue(), qualityScore);
            if (qualityScore >= Config.COMPANY_QUALITY_RELEASE_THRESHOLD.get()) {
                commodityData.ensureCommodity(output.getKey(),
                        Math.max(1L, CapitalismData.getCommodityPrices().getOrDefault(output.getKey(), 1L)));
                commodityData.addSupply(output.getKey(), output.getValue());
            }
        }
        // Cost layers must exist before fulfillment consumes any newly produced
        // stock; otherwise COGS falls back to the market price and the full
        // conversion cost remains incorrectly capitalized in inventory.
        addProducedInventoryCosts(server, company.companyId(), outputs, conversionCost);
        if (qualityScore >= Config.COMPANY_QUALITY_RELEASE_THRESHOLD.get()) {
            for (String itemId : outputs.keySet()) {
                SupplyMarket.fulfill(server,
                        com.ailudick.capitalismmod.market.InventoryOwner.company(company.companyId()),
                        company.ownerUuid(), itemId);
            }
        }
        CompanyProductionBatchSavedData.Batch batch = CompanyProductionBatchSavedData.newBatch(
                company, recipe, conversionCost, qualityScore, recipe.workersPerCycle(),
                server.overworld().getGameTime(), operatingSite,
                stableBatchId);
        CompanyProductionBatchSavedData.get(server).record(batch);
        CompanyQualityControlSavedData.get(server).screen(batch, batch.createdAt());
        CompanyQualityHoldSavedData.get(server).hold(batch);
    }

    /** Game-scale process-quality proxy based on active skill and equipment condition. */
    private static int productionQuality(MinecraftServer server, Company company, MachineType machine) {
        int legacySkill = CompanyLaborSavedData.get(server).averageSkill(company.companyId());
        LaborMarketSavedData laborMarket = LaborMarketSavedData.get(server);
        var marketEmployments = laborMarket.activeForEmployer(company.companyId());
        int marketSkill = (int) Math.round(marketEmployments.stream()
                .map(employment -> laborMarket.profile(employment.workerId()))
                .filter(java.util.Objects::nonNull)
                .mapToInt(LaborProfile::averageSkill).average().orElse(0.0D));
        int marketHealth = (int) Math.round(marketEmployments.stream()
                .map(employment -> laborMarket.profile(employment.workerId()))
                .filter(java.util.Objects::nonNull)
                .mapToInt(LaborProfile::participation).average().orElse(100.0D));
        marketSkill = marketSkill * Math.max(0, Math.min(100, marketHealth)) / 100;
        int skill = legacySkill <= 0 ? marketSkill : marketSkill <= 0 ? legacySkill : (legacySkill + marketSkill) / 2;
        int condition = 100;
        if (machine != null && machine != MachineType.NONE) {
            CompanyEquipmentSavedData.Equipment equipment = CompanyEquipmentSavedData.get(server)
                    .get(company.companyId(), machine);
            condition = equipment == null ? 0 : Math.max(0, Math.min(100, equipment.condition()));
        }
        return Math.max(0, Math.min(100, 40 + skill * 40 / 100 + condition * 20 / 100));
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

    private static boolean siteCanRun(CompanySiteAllocationSavedData allocations, Company company,
                                      CompanySiteSavedData.Site site, MachineType machine) {
        if (site == null) return !allocations.hasMachineAllocation(company.companyId(), machine.id())
                && !allocations.hasWorkerAllocation(company.companyId());
        if (machine != MachineType.NONE && allocations.machineCount(company.companyId(), site, machine.id()) == 0) {
            return false;
        }
        return allocations.workerCount(company.companyId(), site) != 0;
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
        if (company == null) return false;
        String paymentReference = "company-capital:" + company.companyId() + ":"
                + company.registeredCapital() + ":" + amount;
        MinecraftServer server = player.getServer();
        CompanyLedgerSavedData ledger = server == null ? null : CompanyLedgerSavedData.get(server);
        if (ledger != null && hasAppliedCapitalContribution(ledger, company, amount)) {
            // The company leg is already present; reconcile the derived listing
            // snapshot if a stop occurred between those two writes.
            EconomySavedData.get(server).updateListingCapital(stockId(player, name), company.registeredCapital());
            return true;
        }
        // Recovery path for a server stop after the ledger append but before the
        // company registry update. The source is authoritative: finish the
        // missing company-side leg instead of charging the founder again.
        if (ledger != null && ledger.hasSource(company.companyId(), paymentReference)) {
            long recoveredCapital = EconomyMath.add(company.registeredCapital(), amount);
            if (recoveredCapital < 0L || company.treasuryOf(Currencies.USD.id()) > Long.MAX_VALUE - amount) return false;
            Company recovered = company.withRegisteredCapital(recoveredCapital)
                    .addTreasury(Currencies.USD.id(), amount);
            if (recovered != company) setCompany(player, name, recovered);
            EconomySavedData.get(server).updateListingCapital(stockId(player, name), recoveredCapital);
            return true;
        }
        if (!EconomyHelper.tryPayWithReference(player, Currencies.USD, Money.toMinor(amount), paymentReference)) return false;
        long capital = EconomyMath.add(company.registeredCapital(), amount);
        if (capital < 0L || company.treasuryOf(Currencies.USD.id()) > Long.MAX_VALUE - amount) return false;
        Company funded = company.withRegisteredCapital(capital).addTreasury(Currencies.USD.id(), amount);
        if (funded == company) return false;
        if (server != null) {
            ledger.append(new CompanyLedgerEntry(
                    company.companyId(), server.overworld().getGameTime(), "capital_contribution",
                    Currencies.USD.id(), amount, funded.treasuryOf(Currencies.USD.id()),
                    "paid-in capital [source=" + paymentReference + "]"));
            // Record the durable company-side settlement before changing the
            // registry. If the server stops here, the next request can finish it.
            setCompany(player, name, funded);
            EconomySavedData.get(server).updateListingCapital(stockId(player, name), capital);
        } else {
            setCompany(player, name, funded);
        }
        return true;
    }

    private static boolean hasAppliedCapitalContribution(CompanyLedgerSavedData ledger, Company company, long amount) {
        String prefix = "[source=company-capital:" + company.companyId() + ":";
        for (CompanyLedgerEntry entry : ledger.entries(company.companyId())) {
            if (!"capital_contribution".equals(entry.type()) || entry.amount() != amount
                    || entry.description() == null) continue;
            int start = entry.description().indexOf(prefix);
            if (start < 0) continue;
            start += prefix.length();
            int end = entry.description().indexOf(':', start);
            if (end < 0) continue;
            try {
                long previousCapital = Long.parseLong(entry.description().substring(start, end));
                if (EconomyMath.add(previousCapital, amount) == company.registeredCapital()) return true;
            } catch (NumberFormatException ignored) {
                // Ignore malformed legacy descriptions and continue scanning.
            }
        }
        return false;
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

        long now = server.overworld().getGameTime();
        long declarationDay = now / 24000L;
        String declaration = company.companyId() + ":dividend:" + declarationDay + ":" + amountPerShare + ":" + total;
        FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(server);
        if (journal.isCompleted(declaration, "treasury-debit")) {
            recoverDividendPayouts(server);
            return true;
        }
        journal.markStarted(declaration, "dividend", "treasury-debit", Money.toMinorSaturated(total), now);
        Map<String, Long> treasury = new HashMap<>(company.treasury());
        long remaining = company.treasuryOf(Currencies.USD.id()) - total;
        treasury.put(Currencies.USD.id(), remaining);
        CompanySavedData.get(server).put(company.withTreasury(treasury));
        CompanyLedgerSavedData.get(server).append(new CompanyLedgerEntry(
                company.companyId(), now, "dividend_distribution", Currencies.USD.id(),
                -total, remaining, "Dividend declared at USD " + amountPerShare + " per share"));
        journal.markCompleted(declaration, "dividend", "treasury-debit", Money.toMinorSaturated(total), now);

        // The declaration identity must be deterministic: the treasury debit above
        // is the batch boundary, and every shareholder payout can be retried from
        // the same durable receipt after a server interruption.
        for (Map.Entry<UUID, Long> payout : payouts.entrySet()) {
            long minor = Money.toMinor(payout.getValue());
            String source = declaration + ":" + payout.getKey();
            if (!journal.isCompleted(source, "payout")) {
                journal.markStarted(source, "dividend", "payout", minor, now);
            }
            DividendSettlementSavedData.get(server).append(new DividendSettlementSavedData.Payout(
                    source, company.companyId(), payout.getKey(), Currencies.USD.id(), minor, now));
            MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
            mailbox.creditMoneyOnce(payout.getKey(), Currencies.USD.id(), minor, source);
            ServerPlayer online = server.getPlayerList().getPlayer(payout.getKey());
            if (online != null) mailbox.redeemMoneyOnly(online);
            journal.markCompleted(source, "dividend", "payout", minor, now);
            if (!journal.isCompleted(source, "tax")) {
                journal.markStarted(source, "dividend", "tax", minor, now);
                TaxTransactionService.assess(server, TaxType.DIVIDEND, payout.getKey(),
                        Currencies.USD.id(), minor, source, now);
                journal.markCompleted(source, "dividend", "tax", minor, now);
            }
        }
        return true;
    }

    /** Replays persisted dividend payout instructions after an interrupted settlement. */
    public static int recoverDividendPayouts(MinecraftServer server) {
        int recovered = 0;
        DividendSettlementSavedData data = DividendSettlementSavedData.get(server);
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
        for (DividendSettlementSavedData.Payout payout : data.payouts()) {
            boolean credited = mailbox.hasCreditSource(payout.source())
                    || mailbox.creditMoneyOnce(payout.recipient(), payout.currencyId(), payout.amountMinor(), payout.source());
            if (!credited) continue;
            ServerPlayer online = server.getPlayerList().getPlayer(payout.recipient());
            if (online != null) mailbox.redeemMoneyOnly(online);
            FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(server);
            if (!journal.isCompleted(payout.source(), "payout")) {
                journal.markStarted(payout.source(), "dividend", "payout", payout.amountMinor(), payout.gameTime());
                journal.markCompleted(payout.source(), "dividend", "payout", payout.amountMinor(), payout.gameTime());
            }
            if (!journal.isCompleted(payout.source(), "tax")) {
                journal.markStarted(payout.source(), "dividend", "tax", payout.amountMinor(), payout.gameTime());
                TaxTransactionService.assess(server, TaxType.DIVIDEND, payout.recipient(), payout.currencyId(),
                        payout.amountMinor(), payout.source(), payout.gameTime());
                journal.markCompleted(payout.source(), "dividend", "tax", payout.amountMinor(), payout.gameTime());
            }
            recovered++;
        }
        return recovered;
    }

    /** Withdraws {@code amount} of {@code currencyId} from a company's treasury to the founder. */
    public static boolean withdraw(Player player, String name, String currencyId, long amount) {
        if (!(player instanceof ServerPlayer serverPlayer) || !Currencies.exists(currencyId) || amount <= 0) {
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
        CompanyWithdrawalIntentSavedData data = CompanyWithdrawalIntentSavedData.get(player.getServer());
        String id = java.util.UUID.randomUUID().toString();
        data.add(new CompanyWithdrawalIntentSavedData.Intent(id, company.ownerUuid(), company.companyId(), currencyId,
                company.treasuryOf(currencyId), amount));
        settleWithdrawal(serverPlayer, data, data.find(id));
        return data.find(id) == null;
    }

    /** Recovers company treasury withdrawals interrupted before owner delivery. */
    public static int recoverWithdrawals(ServerPlayer player) {
        if (player == null || player.getServer() == null) return 0;
        CompanyWithdrawalIntentSavedData data = CompanyWithdrawalIntentSavedData.get(player.getServer());
        int recovered = 0;
        for (CompanyWithdrawalIntentSavedData.Intent intent : data.intents()) {
            if (player.getUUID().equals(intent.owner()) && settleWithdrawal(player, data, intent)) recovered++;
        }
        return recovered;
    }

    private static boolean settleWithdrawal(ServerPlayer player, CompanyWithdrawalIntentSavedData data,
                                            CompanyWithdrawalIntentSavedData.Intent intent) {
        if (intent == null || !Currencies.exists(intent.currencyId())) return false;
        Company company = CompanySavedData.get(player.getServer()).get(intent.companyId());
        if (company == null || !player.getUUID().equals(company.ownerUuid())) return false;
        long after = intent.balanceBefore() - intent.amount();
        long current = company.treasuryOf(intent.currencyId());
        if (current == intent.balanceBefore()) {
            Map<String, Long> treasury = new HashMap<>(company.treasury());
            treasury.put(intent.currencyId(), after);
            setCompany(player, company.name(), company.withTreasury(treasury));
            CompanyLedgerSavedData.get(player.getServer()).append(new CompanyLedgerEntry(
                    company.companyId(), player.getServer().overworld().getGameTime(), "owner_withdrawal",
                    intent.currencyId(), -intent.amount(), after, "业主提款 [source=" + intent.id() + "]"));
        } else if (current != after) return false;
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(player.getServer());
        String source = "company-withdrawal:" + intent.id();
        if (!mailbox.hasTransferSource(source)) mailbox.creditTransferOnce(intent.owner(), intent.currencyId(),
                Money.toMinor(intent.amount()), source);
        mailbox.redeemTransferOnly(player);
        data.remove(intent.id());
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
        if (company == null || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        TaxSubject subject = new TaxSubject(TaxType.CORPORATE_INCOME, company.companyId(), company.ownerUuid());
        if (company.taxOwed() > 0L) {
            long legacyAmount = Money.toMinor(company.taxOwed());
            TaxService.ensureOutstanding(serverPlayer.getServer(), subject, "usd", legacyAmount,
                    serverPlayer.level().getGameTime(), 0L, 0L);
        }
        boolean paid = false;
        for (var bill : TaxLedgerSavedData.get(serverPlayer.getServer()).bills()) {
            if (!bill.subject().equals(subject) || bill.paid()) continue;
            if (!bill.declared()) {
                TaxLedgerSavedData.get(serverPlayer.getServer()).replace(
                        bill.withDeclaration(serverPlayer.level().getGameTime(), player.getUUID().toString()));
                bill = TaxLedgerSavedData.get(serverPlayer.getServer()).get(bill.id());
            }
            paid |= TaxService.payFromCompany(serverPlayer.getServer(), company, bill.id(), bill.outstanding());
        }
        return paid;
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
        Company buyerCompany = getCompany(buyer, offer.companyName());
        boolean alreadyTransferred = company == null && buyerCompany != null
                && buyerCompany.ownerUuid().equals(buyer.getUUID()) && !isListed(buyer, offer.companyName());
        if ((!alreadyTransferred && company == null) || (!alreadyTransferred && buyerCompany != null)
                || isListed(seller, offer.companyName())) {
            return false;
        }
        String payoutSource = "company-acquisition:" + offer.id() + ":seller-payout";
        String debitReference = "company-acquisition:" + offer.id() + ":buyer-payment";
        boolean alreadyDebited = EconomyLogSavedData.get(buyer.getServer())
                .hasReference(buyer.getUUID(), debitReference) || offer.buyerPaid();
        if (!alreadyTransferred && !alreadyDebited
                && !EconomyHelper.tryPayWithReference(buyer, Currencies.USD,
                Money.toMinor(offer.price()), debitReference)) {
            return false;
        }
        if (!alreadyTransferred && !offer.buyerPaid()) {
            AcquisitionSavedData.get(buyer.getServer()).markBuyerPaid(offer.id());
        }
        if (!alreadyTransferred) {
            removeCompany(seller, company.name());
            putCompany(buyer, company.name(), company);
        }
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(buyer.getServer());
        mailbox.creditMoneyOnce(seller.getUUID(), Currencies.USD.id(), Money.toMinor(offer.price()), payoutSource);
        mailbox.redeemMoneyOnly(seller);
        TaxTransactionService.assess(buyer.getServer(), TaxType.CAPITAL_GAINS, seller.getUUID(), Currencies.USD.id(),
                Money.toMinorSaturated(offer.price()), "company-acquisition:" + offer.id(),
                buyer.getServer().overworld().getGameTime());
        return true;
    }

    /** Completes a previously paid acquisition while the buyer is offline. */
    public static boolean acquirePaidOfflineBuyer(ServerPlayer seller, AcquisitionSavedData.Offer offer) {
        if (seller == null || offer == null || !offer.buyerPaid()
                || !offer.sellerUuid().equals(seller.getUUID())
                || offer.buyerUuid().equals(seller.getUUID()) || offer.price() <= 0) return false;
        MinecraftServer server = seller.getServer();
        Company existing = findCompany(server, offer.buyerUuid(), offer.companyName());
        Company company = getCompany(seller, offer.companyName());
        if (company == null && existing != null && !offer.companyId().isBlank()
                && offer.companyId().equals(existing.companyId())) {
            settleAcquisitionPayout(server, seller, offer);
            return true;
        }
        if (company == null || isListed(seller, offer.companyName())) return false;
        if (existing != null && !existing.companyId().equals(company.companyId())) return false;
        if (existing == null) {
            CompanySavedData.get(server).put(company.withIdentity(company.companyId(), offer.buyerUuid()));
            detachCompany(seller, offer.companyName());
        } else if (!offer.buyerUuid().equals(existing.ownerUuid())) {
            return false;
        }
        settleAcquisitionPayout(server, seller, offer);
        return true;
    }

    private static void settleAcquisitionPayout(MinecraftServer server, ServerPlayer seller,
                                                  AcquisitionSavedData.Offer offer) {
        String payoutSource = "company-acquisition:" + offer.id() + ":seller-payout";
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
        mailbox.creditMoneyOnce(seller.getUUID(), Currencies.USD.id(), Money.toMinor(offer.price()), payoutSource);
        mailbox.redeemMoneyOnly(seller);
        TaxTransactionService.assess(server, TaxType.CAPITAL_GAINS, seller.getUUID(), Currencies.USD.id(),
                Money.toMinorSaturated(offer.price()), "company-acquisition:" + offer.id(),
                server.overworld().getGameTime());
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
            CompanyPayrollSavedData.get(server).transferCompany(source.companyId(), target.companyId());
            CompanyFreightContractSavedData.get(server).transferCompany(source.companyId(), target.companyId());
            CompanyFreightSettlementSavedData.get(server).transferCompany(source.companyId(), target.companyId());
            CompanyLogisticsCostSavedData.get(server).transferCompany(source.companyId(), target.companyId());
            CompanyLedgerSavedData.get(server).transferCompany(source.companyId(), target.companyId());
            CompanyServiceDeliverySavedData.get(server).transferCompany(source.companyId(), target.companyId());
            CompanySiteSavedData siteData = CompanySiteSavedData.get(server);
            siteData.transferCompany(source.companyId(), target.companyId());
            CompanySiteAllocationSavedData.get(server).transferCompany(source.companyId(), target.companyId(),
                    siteData.sites(target.companyId()));
            CompanyInventoryCostSavedData.get(server).transferCompany(source.companyId(), target.companyId());
            CompanyQualitySavedData.get(server).transferCompany(source.companyId(), target.companyId());
            CompanyProductionSavedData.get(server).mergeCompany(source.companyId(), target.companyId());
            CompanyProductionBatchSavedData.get(server).mergeCompany(source.companyId(), target.companyId());
            CompanyQualityControlSavedData.get(server).mergeCompany(source.companyId(), target.companyId());
            CompanyQualityHoldSavedData.get(server).mergeCompany(source.companyId(), target.companyId());
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
        long now = buyer.getServer().overworld().getGameTime();
        FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(buyer.getServer());
        String transactionId = "public-takeover:" + offer.id();
        String transferSource = "public-takeover:" + offer.id() + ":shares";
        String paymentReference = "public-takeover:" + offer.id() + ":buyer-payment";
        if (!journal.isCompleted(transactionId, "payment")) {
            journal.markStarted(transactionId, "public-takeover", "payment", Money.toMinorSaturated(total), now);
        }
        boolean alreadyDebited = EconomyLogSavedData.get(buyer.getServer())
                .hasReference(buyer.getUUID(), paymentReference);
        if (total < 0 || (!data.hasShareTransfer(transferSource) && !alreadyDebited
                && !EconomyHelper.tryPayWithReference(buyer, Currencies.USD,
                Money.toMinor(total), paymentReference))) {
            return false;
        }
        journal.markCompleted(transactionId, "public-takeover", "payment", Money.toMinorSaturated(total), now);
        if (!journal.isCompleted(transactionId, "shares")) {
            journal.markStarted(transactionId, "public-takeover", "shares", offer.quantity(), now);
        }
        if (!data.hasShareTransfer(transferSource)
                && !data.transferSharesOnce(offer.stockId(), seller.getUUID(), buyer.getUUID(), offer.quantity(), transferSource)) {
            return false;
        }
        journal.markCompleted(transactionId, "public-takeover", "shares", offer.quantity(), now);
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(buyer.getServer());
        String payoutSource = "public-takeover:" + offer.id() + ":payout";
        if (!journal.isCompleted(transactionId, "seller-payout")) {
            journal.markStarted(transactionId, "public-takeover", "seller-payout", Money.toMinorSaturated(total), now);
        }
        mailbox.creditMoneyOnce(seller.getUUID(), Currencies.USD.id(), Money.toMinor(total), payoutSource);
        mailbox.redeemMoneyOnly(seller);
        journal.markCompleted(transactionId, "public-takeover", "seller-payout", Money.toMinorSaturated(total), now);
        if (!journal.isCompleted(transactionId, "tax")) {
            journal.markStarted(transactionId, "public-takeover", "tax", Money.toMinorSaturated(total), now);
            TaxTransactionService.assess(buyer.getServer(), TaxType.CAPITAL_GAINS, seller.getUUID(), Currencies.USD.id(),
                    Money.toMinorSaturated(total), transactionId, now);
            journal.markCompleted(transactionId, "public-takeover", "tax", Money.toMinorSaturated(total), now);
        }
        return true;
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
            CompanySiteSavedData.get(player.getServer()).remove(removed);
            CompanySiteAllocationSavedData.get(player.getServer()).remove(removed);
            CompanySavedData.get(player.getServer()).remove(removed);
        }
    }

    private static void detachCompany(Player player, String name) {
        Conglomerate conglomerate = getConglomerate(player);
        Map<String, String> companies = new HashMap<>(conglomerate.companies());
        companies.remove(name);
        player.setData(ModAttachments.CONGLOMERATE, new Conglomerate(conglomerate.name(), companies));
    }
}
