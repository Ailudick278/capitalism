package com.ailudick.capitalismmod.futures;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.market.Commodities;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * World-persisted futures market state: per-commodity futures price, net volume,
 * expiry day, a rolling day counter, each player's margin balance, and open positions.
 */
public final class FuturesSavedData extends SavedData {
    private static final String ID = "capitalismmod_futures";

    private final Map<String, Long> futuresPrice = new HashMap<>();
    private final Map<String, Long> netVolume = new HashMap<>();
    private final Map<String, Long> expiryDay = new HashMap<>();
    private long dayCounter = 0L;
    // player uuid string -> margin balance (USD major units)
    private final Map<String, Long> marginBalance = new HashMap<>();
    private final List<Position> positions = new ArrayList<>();

    private record State(
            Map<String, Long> futuresPrice,
            Map<String, Long> netVolume,
            Map<String, Long> expiryDay,
            long dayCounter,
            Map<String, Long> marginBalance,
            List<Position> positions) {
        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("futuresPrice").forGetter(State::futuresPrice),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("netVolume").forGetter(State::netVolume),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("expiryDay").forGetter(State::expiryDay),
                Codec.LONG.fieldOf("dayCounter").forGetter(State::dayCounter),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("marginBalance").forGetter(State::marginBalance),
                Position.CODEC.listOf().fieldOf("positions").forGetter(State::positions)
        ).apply(instance, State::new));
    }

    private FuturesSavedData() {
        seed();
    }

    public static FuturesSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(FuturesSavedData::new, FuturesSavedData::load), ID);
    }

    private void seed() {
        for (ItemStack stack : Commodities.ALL) {
            ensureContract(Commodities.id(stack), Math.max(1, Commodities.initialPriceOf(stack)));
        }
    }

    public void ensureContract(String itemId, long initialPrice) {
        futuresPrice.putIfAbsent(itemId, initialPrice);
        netVolume.putIfAbsent(itemId, 0L);
        expiryDay.putIfAbsent(itemId, (long) Config.FUTURES_EXPIRY_DAYS.get());
    }

    // ---- prices ----

    public long price(String itemId) {
        return futuresPrice.getOrDefault(itemId, 0L);
    }

    public Map<String, Long> prices() {
        return futuresPrice;
    }

    public void putPrice(String itemId, long price) {
        futuresPrice.put(itemId, price);
    }

    // ---- volume ----

    public long netVolume(String itemId) {
        return netVolume.getOrDefault(itemId, 0L);
    }

    public void addNetVolume(String itemId, long delta) {
        netVolume.merge(itemId, delta, Long::sum);
    }

    public void resetNetVolume(String itemId) {
        netVolume.put(itemId, 0L);
    }

    // ---- expiry & day counter ----

    public long expiryDay(String itemId) {
        return expiryDay.getOrDefault(itemId, 0L);
    }

    public void setExpiryDay(String itemId, long day) {
        expiryDay.put(itemId, day);
    }

    public long dayCounter() {
        return dayCounter;
    }

    public void incrementDay() {
        dayCounter++;
    }

    // ---- margin balance ----

    public long marginBalance(UUID playerId) {
        return marginBalance.getOrDefault(playerId.toString(), 0L);
    }

    public void setMarginBalance(UUID playerId, long balance) {
        if (balance <= 0) {
            marginBalance.remove(playerId.toString());
        } else {
            marginBalance.put(playerId.toString(), balance);
        }
    }

    public void addMarginBalance(UUID playerId, long delta) {
        setMarginBalance(playerId, marginBalance(playerId) + delta);
    }

    // ---- positions ----

    public List<Position> positions() {
        return positions;
    }

    public void addPosition(Position position) {
        positions.add(position);
    }

    public void removePosition(String positionId) {
        positions.removeIf(position -> position.id().equals(positionId));
    }

    public void replacePosition(Position position) {
        for (int i = 0; i < positions.size(); i++) {
            if (positions.get(i).id().equals(position.id())) {
                positions.set(i, position);
                return;
            }
        }
    }

    public Position findPosition(String positionId) {
        for (Position position : positions) {
            if (position.id().equals(positionId)) {
                return position;
            }
        }
        return null;
    }

    // ---- persistence ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State state = new State(new HashMap<>(futuresPrice), new HashMap<>(netVolume), new HashMap<>(expiryDay),
                dayCounter, new HashMap<>(marginBalance), new ArrayList<>(positions));
        State.CODEC.encodeStart(NbtOps.INSTANCE, state).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static FuturesSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FuturesSavedData data = new FuturesSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result().ifPresent(state -> {
                data.futuresPrice.putAll(state.futuresPrice());
                data.netVolume.putAll(state.netVolume());
                data.expiryDay.putAll(state.expiryDay());
                data.dayCounter = state.dayCounter();
                data.marginBalance.putAll(state.marginBalance());
                data.positions.addAll(state.positions());
            });
        }
        return data;
    }
}
