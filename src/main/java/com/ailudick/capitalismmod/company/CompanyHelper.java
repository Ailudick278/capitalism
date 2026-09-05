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
        return true;
    }

    /** Records a confirmed company revenue event and assesses corporate income tax once. */
    public static boolean recordTaxableIncome(MinecraftServer server, Company company, String sourceId,
                                              long revenue, String currencyId, long occurredAt) {
        if (server == null || company == null || sourceId == null || sourceId.isBlank() || revenue <= 0L) return false;
        long quarter = 90L * 24000L;
        long periodEnd = ((occurredAt / quarter) + 1L) * quarter;
        long periodStart = periodEnd - quarter;
        CorporateTaxPeriodSavedData.get(server).record(company.companyId() + ":" + sourceId,
                company.companyId(), currencyId, revenue, periodStart, periodEnd);
        long year = 360L * 24000L;
        long yearEnd = ((occurredAt / year) + 1L) * year;
        CorporateTaxAnnualSavedData.get(server).record(company.companyId(), currencyId, revenue,
                yearEnd - year, yearEnd);
        return true;
    }

    /** Records a deductible business expense for the same corporate tax periods. */
    public static boolean recordTaxableExpense(MinecraftServer server, Company company, String sourceId,
                                                long expense, String currencyId, long occurredAt) {
        if (server == null || company == null || sourceId == null || sourceId.isBlank() || expense <= 0L) return false;
        long quarter = 90L * 24000L;
        long periodEnd = ((occurredAt / quarter) + 1L) * quarter;
        long periodStart = periodEnd - quarter;
        CorporateTaxPeriodSavedData.get(server).recordExpense(company.companyId() + ":expense:" + sourceId,
                company.companyId(), currencyId, expense, periodStart, periodEnd);
        long year = 360L * 24000L;
        long yearEnd = ((occurredAt / year) + 1L) * year;
        CorporateTaxAnnualSavedData.get(server).recordExpense(company.companyId(), currencyId, expense,
                yearEnd - year, yearEnd);
        TaxExpenseService.record(server, company.ownerUuid(), company.companyId(), "business_expense",
                currencyId, expense, occurredAt, company.companyId() + ":expense:" + sourceId, true);
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

    /** Consumes the company's inputs from the warehouse, recording demand. Returns false if any input is short. */
    private static boolean consumeInputs(MinecraftServer server, Player player, Company company) {
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
    private static void produceOutputs(MinecraftServer server, Player player, Company company) {
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
            commodityData.addSupply(output.getKey(), output.getValue());
            // automatically fulfill any backorders for this commodity
            SupplyMarket.fulfill(server,
                    com.ailudick.capitalismmod.market.InventoryOwner.company(company.companyId()),
                    player.getUUID(), output.getKey());
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
        long totalShares = EconomyMath.multiply(1000L, company.level());
        if (totalShares < 0) {
            return false;
        }
        data.list(stockId, company.name(), company.level(), totalShares);
        data.addShares(stockId, player.getUUID(), totalShares);
        return true;
    }

    /** Upgrades a company: pays the upgrade cost from the founder, then raises its level. */
    public static boolean upgrade(Player player, String name) {
        Company company = getCompany(player, name);
        if (company == null) {
            return false;
        }
        long cost = CompanyEconomy.upgradeCost(company.level());
        if (cost < 0 || !EconomyHelper.tryPay(player, Currencies.USD, Money.toMinor(cost))) {
            return false;
        }
        setCompany(player, name, company.withLevel(company.level() + 1));
        MinecraftServer server = player.getServer();
        if (server != null) {
            recordTaxableExpense(server, company, "upgrade:" + company.level(), cost, Currencies.USD.id(),
                    player.level().getGameTime());
            EconomySavedData.get(server).updateListingLevel(stockId(player, name), company.level() + 1);
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
                setCompany(player, entry.getKey(), entry.getValue().withTaxOwed(Math.max(0L, outstandingMinor / 100L)));
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
        int level = Math.min(Integer.MAX_VALUE, Math.max(1, source.level()) + Math.max(1, target.level()));
        MinecraftServer server = player.getServer();
        if (server != null) {
            WarehouseSavedData.get(server).transferAll(
                    com.ailudick.capitalismmod.market.InventoryOwner.company(source.companyId()),
                    com.ailudick.capitalismmod.market.InventoryOwner.company(target.companyId()));
        }
        Map<String, Long> treasury = new HashMap<>(target.treasury());
        for (Map.Entry<String, Long> entry : source.treasury().entrySet()) {
            treasury.merge(entry.getKey(), entry.getValue(), (a, b) -> EconomyMath.add(a, b));
        }
        Company merged = new Company(target.companyId(), player.getUUID(), target.name(), target.type(), level, treasury,
                EconomyMath.add(target.taxOwed(), source.taxOwed()));
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
