package com.ailudick.capitalismmod.economy;

import com.ailudick.capitalismmod.stock.Candle;
import com.ailudick.capitalismmod.stock.Stock;
import com.ailudick.capitalismmod.stock.StockOrder;
import com.ailudick.capitalismmod.stock.Stocks;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * World-persisted economic state shared across all players: stock prices, candle
 * history, share holdings, and the registry of listed (IPO'd) companies.
 *
 * <p>Stock ids are either abstract stocks from config (see {@link Stocks}) or
 * listed companies, keyed by {@code "<ownerUuid>:<companyName>"}.
 */
public final class EconomySavedData extends SavedData {
    private static final String ID = "capitalismmod_economy";
    private static final int MAX_CANDLES = 30;

    /** Fundamental value = level * FUNDAMENTAL_PER_LEVEL for listed companies. */
    public static final long FUNDAMENTAL_PER_LEVEL = 100L;

    private final Map<String, Long> prices = new HashMap<>();
    private final Map<String, Long> netVolume = new HashMap<>();
    private final Map<String, List<Candle>> history = new HashMap<>();
    // stockId -> (playerUuid string -> shares)
    private final Map<String, Map<String, Long>> shareholders = new HashMap<>();
    // company stockId -> listing snapshot (name, level, total shares)
    private final Map<String, Listing> listings = new HashMap<>();
    // stock exchange limit orders
    private final List<StockOrder> orders = new ArrayList<>();
    // stockId -> previous close (anchor for the daily price limit band)
    private final Map<String, Long> prevClose = new HashMap<>();

    /** Snapshot of a listed company, kept so its stock stays visible while the founder is offline. */
    public record Listing(String name, int level, long totalShares) {
        public static final Codec<Listing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Listing::name),
                Codec.INT.fieldOf("level").forGetter(Listing::level),
                Codec.LONG.fieldOf("totalShares").forGetter(Listing::totalShares)
        ).apply(instance, Listing::new));
    }

    private record State(
            Map<String, Long> prices,
            Map<String, Long> netVolume,
            Map<String, List<Candle>> history,
            Map<String, Map<String, Long>> shareholders,
            Map<String, Listing> listings,
            List<StockOrder> orders,
            Map<String, Long> prevClose) {
        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("prices").forGetter(State::prices),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("netVolume").forGetter(State::netVolume),
                Codec.unboundedMap(Codec.STRING, Candle.CODEC.listOf()).fieldOf("history").forGetter(State::history),
                Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.LONG)).fieldOf("shareholders").forGetter(State::shareholders),
                Codec.unboundedMap(Codec.STRING, Listing.CODEC).fieldOf("listings").forGetter(State::listings),
                StockOrder.CODEC.listOf().fieldOf("orders").forGetter(State::orders),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("prevClose").forGetter(State::prevClose)
        ).apply(instance, State::new));
    }

    private EconomySavedData() {
        seedAbstractStocks();
    }

    public static EconomySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(EconomySavedData::new, EconomySavedData::load), ID);
    }

    private void seedAbstractStocks() {
        for (Stock stock : Stocks.ALL) {
            ensureStock(stock.id(), stock.initialPrice());
        }
    }

    // ---- stocks ----

    public boolean isStock(String stockId) {
        return Stocks.exists(stockId) || listings.containsKey(stockId);
    }

    public long price(String stockId) {
        return prices.getOrDefault(stockId, 0L);
    }

    public Map<String, Long> prices() {
        return prices;
    }

    public Map<String, List<Candle>> history() {
        return history;
    }

    public long fundamental(String stockId) {
        Listing listing = listings.get(stockId);
        if (listing != null) {
            return EconomyMath.multiply(FUNDAMENTAL_PER_LEVEL, listing.level());
        }
        Stock stock = Stocks.byId(stockId);
        return stock != null ? stock.initialPrice() : 0L;
    }

    public void ensureStock(String stockId, long initialPrice) {
        prices.putIfAbsent(stockId, initialPrice);
        netVolume.putIfAbsent(stockId, 0L);
        history.computeIfAbsent(stockId, k -> new ArrayList<>());
        prevClose.putIfAbsent(stockId, initialPrice);
    }

    public void putPrice(String stockId, long price) {
        prices.put(stockId, price);
    }

    public void addCandle(String stockId, Candle candle) {
        List<Candle> candles = history.computeIfAbsent(stockId, k -> new ArrayList<>());
        candles.add(candle);
        while (candles.size() > MAX_CANDLES) {
            candles.remove(0);
        }
    }

    public long netVolume(String stockId) {
        return netVolume.getOrDefault(stockId, 0L);
    }

    public void addNetVolume(String stockId, long delta) {
        netVolume.merge(stockId, delta, Long::sum);
    }

    public void resetNetVolume(String stockId) {
        netVolume.put(stockId, 0L);
    }

    // ---- listings ----

    public boolean isListed(String stockId) {
        return listings.containsKey(stockId);
    }

    public Map<String, Listing> listings() {
        return listings;
    }

    public Map<String, String> listingNames() {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Listing> entry : listings.entrySet()) {
            result.put(entry.getKey(), entry.getValue().name());
        }
        return result;
    }

    public void list(String stockId, String name, int level, long totalShares) {
        listings.put(stockId, new Listing(name, level, totalShares));
        ensureStock(stockId, EconomyMath.multiply(FUNDAMENTAL_PER_LEVEL, level));
        setDirty();
    }

    public void updateListingLevel(String stockId, int level) {
        Listing listing = listings.get(stockId);
        if (listing != null && listing.level() != level) {
            listings.put(stockId, new Listing(listing.name(), level, listing.totalShares()));
            setDirty();
        }
    }

    // ---- shareholdings ----

    public long holdings(String stockId, UUID playerId) {
        Map<String, Long> holders = shareholders.get(stockId);
        return holders == null ? 0L : holders.getOrDefault(playerId.toString(), 0L);
    }

    public Map<String, Map<String, Long>> shareholders() {
        return shareholders;
    }

    /** Aggregates all non-zero holdings into a stockId -> shares map for one player. */
    public Map<String, Long> portfolio(UUID playerId) {
        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Long>> entry : shareholders.entrySet()) {
            Long shares = entry.getValue().get(playerId.toString());
            if (shares != null && shares != 0) {
                result.put(entry.getKey(), shares);
            }
        }
        return result;
    }

    public void addShares(String stockId, UUID playerId, long delta) {
        Map<String, Long> holders = shareholders.computeIfAbsent(stockId, k -> new HashMap<>());
        String key = playerId.toString();
        long next = EconomyMath.add(holders.getOrDefault(key, 0L), delta);
        if (next < 0) {
            next = 0;
        }
        if (next == 0) {
            holders.remove(key);
        } else {
            holders.put(key, next);
        }
        if (holders.isEmpty()) {
            shareholders.remove(stockId);
        }
        setDirty();
    }

    // ---- orders ----

    public List<StockOrder> orders() {
        return orders;
    }

    public void addOrder(StockOrder order) {
        orders.add(order);
    }

    public void removeOrder(String orderId) {
        orders.removeIf(order -> order.id().equals(orderId));
    }

    public void replaceOrder(StockOrder order) {
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).id().equals(order.id())) {
                orders.set(i, order);
                return;
            }
        }
    }

    public StockOrder findOrder(String orderId) {
        for (StockOrder order : orders) {
            if (order.id().equals(orderId)) {
                return order;
            }
        }
        return null;
    }

    // ---- previous close (price limit band) ----

    public long prevClose(String stockId) {
        return prevClose.getOrDefault(stockId, 0L);
    }

    public void setPrevClose(String stockId, long price) {
        prevClose.put(stockId, price);
    }

    // ---- persistence ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State state = new State(new HashMap<>(prices), new HashMap<>(netVolume), new HashMap<>(history),
                new HashMap<>(shareholders), new HashMap<>(listings), new ArrayList<>(orders), new HashMap<>(prevClose));
        State.CODEC.encodeStart(NbtOps.INSTANCE, state).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static EconomySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EconomySavedData data = new EconomySavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result().ifPresent(state -> {
                data.prices.putAll(state.prices());
                data.netVolume.putAll(state.netVolume());
                state.history().forEach((k, v) -> data.history.put(k, new ArrayList<>(v)));
                state.shareholders().forEach((k, v) -> data.shareholders.put(k, new HashMap<>(v)));
                data.listings.putAll(state.listings());
                data.orders.addAll(state.orders());
                data.prevClose.putAll(state.prevClose());
            });
        }
        return data;
    }
}
