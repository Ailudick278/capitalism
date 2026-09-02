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
        return getConglomerate(player).companies();
    }

    public static Company getCompany(Player player, String name) {
        return getCompanies(player).get(name);
    }

    public static boolean exists(Player player, String name) {
        return getCompanies(player).containsKey(name);
    }

    public static String stockId(Player player, String name) {
        return player.getStringUUID() + ":" + name;
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

        Map<String, Company> updated = new HashMap<>(conglomerate.companies());
        updated.put(trimmed, Company.create(trimmed, type));
        player.setData(ModAttachments.CONGLOMERATE, new Conglomerate(conglomerate.name(), updated));
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

    /** Accrues one income tick: unlisted companies bank it; listed companies pay it out as dividends. */
    public static void accrueIncome(Player player) {
        MinecraftServer server = player.getServer();
        Conglomerate conglomerate = getConglomerate(player);
        Map<String, Company> companies = conglomerate.companies();
        if (companies.isEmpty()) {
            return;
        }
        EconomySavedData data = server != null ? EconomySavedData.get(server) : null;

        Map<String, Company> updated = new HashMap<>();
        boolean changed = false;
        for (Map.Entry<String, Company> entry : companies.entrySet()) {
            Company company = entry.getValue();
            long income = CompanyEconomy.incomePerTick(company, player);
            if (income <= 0) {
                updated.put(entry.getKey(), company);
                continue;
            }
            if (!canProduceOutputs(server, company)) {
                updated.put(entry.getKey(), company);
                continue;
            }
            if (!consumeInputs(server, player, company)) {
                updated.put(entry.getKey(), company);
                continue;
            }
            produceOutputs(server, player, company);
            long tax = safeRate(income, Config.INCOME_TAX_RATE.get());
            long maintenance = EconomyMath.multiply(Config.COMPANY_MAINTENANCE_PER_LEVEL.get(), company.level());
            if (maintenance < 0) {
                maintenance = Long.MAX_VALUE;
            }
            long net = income > tax ? income - tax : 0L;
            net = net > maintenance ? net - maintenance : 0L;
            Company taxed = company.addTaxOwed(tax);
            if (data != null && data.isListed(stockId(player, company.name()))) {
                distributeDividend(server, stockId(player, company.name()), net);
            } else {
                taxed = taxed.addTreasury("usd", net);
            }
            updated.put(entry.getKey(), taxed);
            changed = true;
        }
        if (changed) {
            player.setData(ModAttachments.CONGLOMERATE, new Conglomerate(conglomerate.name(), updated));
        }
    }

    /** Consumes the company's inputs from the warehouse, recording demand. Returns false if any input is short. */
    private static boolean consumeInputs(MinecraftServer server, Player player, Company company) {
        if (server == null) {
            return true;
        }
        Map<String, Integer> inputs = CompanyEconomy.inputs(company);
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        for (Map.Entry<String, Integer> input : inputs.entrySet()) {
            Item item = parseItem(input.getKey());
            if (item == null || input.getValue() <= 0
                    || warehouse.count(player.getUUID(), input.getKey()) < input.getValue()) {
                return false;
            }
        }
        CommoditySavedData commodityData = CommoditySavedData.get(server);
        for (Map.Entry<String, Integer> input : inputs.entrySet()) {
            Item item = parseItem(input.getKey());
            warehouse.consume(player.getUUID(), item, input.getValue());
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
        CommoditySavedData commodityData = CommoditySavedData.get(server);
        for (Map.Entry<String, Integer> output : CompanyEconomy.outputs(company).entrySet()) {
            Item item = parseItem(output.getKey());
            if (item == null || output.getValue() <= 0) {
                continue;
            }
            warehouse.credit(player.getUUID(), item, output.getValue());
            commodityData.addSupply(output.getKey(), output.getValue());
            // automatically fulfill any backorders for this commodity
            SupplyMarket.fulfill(server, player.getUUID(), output.getKey());
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

    /** Pays a company's accrued corporate income tax, deducting it from the founder's wallet. */
    public static boolean payTax(Player player, String name) {
        Company company = getCompany(player, name);
        if (company == null || company.taxOwed() <= 0) {
            return false;
        }
        if (!EconomyHelper.tryPay(player, Currencies.USD, Money.toMinor(company.taxOwed()))) {
            return false;
        }
        setCompany(player, name, company.withTaxOwed(0));
        return true;
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
        Map<String, Long> treasury = new HashMap<>(target.treasury());
        for (Map.Entry<String, Long> entry : source.treasury().entrySet()) {
            treasury.merge(entry.getKey(), entry.getValue(), (a, b) -> EconomyMath.add(a, b));
        }
        Company merged = new Company(target.name(), target.type(), level, treasury,
                EconomyMath.add(target.taxOwed(), source.taxOwed()));
        removeCompany(player, sourceName);
        putCompany(player, targetName, merged);
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
        Conglomerate conglomerate = getConglomerate(player);
        Map<String, Company> companies = new HashMap<>(conglomerate.companies());
        companies.put(name, company);
        player.setData(ModAttachments.CONGLOMERATE, new Conglomerate(conglomerate.name(), companies));
    }

    private static void putCompany(Player player, String name, Company company) {
        Map<String, Company> companies = new HashMap<>(getCompanies(player));
        companies.put(name, company);
        Conglomerate conglomerate = getConglomerate(player);
        player.setData(ModAttachments.CONGLOMERATE, new Conglomerate(conglomerate.name(), companies));
    }

    private static void removeCompany(Player player, String name) {
        Map<String, Company> companies = new HashMap<>(getCompanies(player));
        companies.remove(name);
        Conglomerate conglomerate = getConglomerate(player);
        player.setData(ModAttachments.CONGLOMERATE, new Conglomerate(conglomerate.name(), companies));
    }
}
