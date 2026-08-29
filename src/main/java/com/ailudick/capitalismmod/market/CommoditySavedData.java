package com.ailudick.capitalismmod.market;

import com.ailudick.capitalismmod.stock.Candle;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * World-persisted state for the commodity exchange: the limit order book, current
 * prices, net volume, net supply/demand, previous close, and candle history per
 * commodity. Commodities are keyed by item id (see {@link Commodities#id(ItemStack)}).
 */
public final class CommoditySavedData extends SavedData {
    private static final String ID = "capitalismmod_commodity";
    private static final int MAX_CANDLES = 30;

    private final List<MarketOrder> orders = new ArrayList<>();
    private final Map<String, Long> prices = new HashMap<>();
    private final Map<String, Long> netVolume = new HashMap<>();
    private final Map<String, List<Candle>> history = new HashMap<>();
    private final Map<String, Long> supply = new HashMap<>();
    private final Map<String, Long> prevClose = new HashMap<>();

    private record State(
            List<MarketOrder> orders,
            Map<String, Long> prices,
            Map<String, Long> netVolume,
            Map<String, List<Candle>> history,
            Map<String, Long> supply,
            Map<String, Long> prevClose) {
        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                MarketOrder.CODEC.listOf().fieldOf("orders").forGetter(State::orders),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("prices").forGetter(State::prices),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("netVolume").forGetter(State::netVolume),
                Codec.unboundedMap(Codec.STRING, Candle.CODEC.listOf()).fieldOf("history").forGetter(State::history),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("supply").forGetter(State::supply),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("prevClose").forGetter(State::prevClose)
        ).apply(instance, State::new));
    }

    private CommoditySavedData() {
        seed();
    }

    public static CommoditySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CommoditySavedData::new, CommoditySavedData::load), ID);
    }

    private void seed() {
        for (ItemStack stack : Commodities.ALL) {
            ensureCommodity(Commodities.id(stack), Math.max(1, Commodities.initialPriceOf(stack)));
        }
    }

    // ---- prices & candles ----

    public long price(String itemId) {
        return prices.getOrDefault(itemId, 0L);
    }

    public Map<String, Long> prices() {
        return prices;
    }

    public Map<String, List<Candle>> history() {
        return history;
    }

    public long fundamental(String itemId) {
        return Math.max(1, Commodities.initialPrice(itemId));
    }

    public void ensureCommodity(String itemId, long initialPrice) {
        prices.putIfAbsent(itemId, initialPrice);
        netVolume.putIfAbsent(itemId, 0L);
        history.computeIfAbsent(itemId, k -> new ArrayList<>());
        supply.putIfAbsent(itemId, 0L);
        prevClose.putIfAbsent(itemId, initialPrice);
    }

    public void putPrice(String itemId, long price) {
        prices.put(itemId, price);
    }

    public void addCandle(String itemId, Candle candle) {
        List<Candle> candles = history.computeIfAbsent(itemId, k -> new ArrayList<>());
        candles.add(candle);
        while (candles.size() > MAX_CANDLES) {
            candles.remove(0);
        }
    }

    public long netVolume(String itemId) {
        return netVolume.getOrDefault(itemId, 0L);
    }

    public void addNetVolume(String itemId, long delta) {
        netVolume.merge(itemId, delta, Long::sum);
    }

    public void resetNetVolume(String itemId) {
        netVolume.put(itemId, 0L);
    }

    // ---- supply / demand (production +, consumption -) ----

    public long supply(String itemId) {
        return supply.getOrDefault(itemId, 0L);
    }

    public void addSupply(String itemId, long delta) {
        supply.merge(itemId, delta, Long::sum);
    }

    public void resetSupply(String itemId) {
        supply.put(itemId, 0L);
    }

    // ---- previous close (price limit band) ----

    public long prevClose(String itemId) {
        return prevClose.getOrDefault(itemId, 0L);
    }

    public void setPrevClose(String itemId, long price) {
        prevClose.put(itemId, price);
    }

    // ---- orders ----

    public List<MarketOrder> orders() {
        return orders;
    }

    public void addOrder(MarketOrder order) {
        orders.add(order);
    }

    public void removeOrder(String orderId) {
        orders.removeIf(order -> order.id().equals(orderId));
    }

    public void replaceOrder(MarketOrder order) {
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).id().equals(order.id())) {
                orders.set(i, order);
                return;
            }
        }
    }

    public MarketOrder findOrder(String orderId) {
        for (MarketOrder order : orders) {
            if (order.id().equals(orderId)) {
                return order;
            }
        }
        return null;
    }

    // ---- persistence ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State state = new State(new ArrayList<>(orders), new HashMap<>(prices), new HashMap<>(netVolume),
                new HashMap<>(history), new HashMap<>(supply), new HashMap<>(prevClose));
        State.CODEC.encodeStart(NbtOps.INSTANCE, state).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CommoditySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CommoditySavedData data = new CommoditySavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result().ifPresent(state -> {
                data.prices.putAll(state.prices());
                data.netVolume.putAll(state.netVolume());
                state.history().forEach((k, v) -> data.history.put(k, new ArrayList<>(v)));
                data.supply.putAll(state.supply());
                data.prevClose.putAll(state.prevClose());
                for (MarketOrder order : state.orders()) {
                    // Drop orders whose commodity no longer resolves (config changed).
                    if (!order.commodity().is(Items.AIR)) {
                        data.orders.add(order);
                    }
                }
            });
        }
        return data;
    }
}
