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
import java.util.HashSet;
import java.util.Set;
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
    private long lastSettlementDay = -1L;
    // player uuid string -> margin balance (USD major units)
    private final Map<String, Long> marginBalance = new HashMap<>();
    private final List<Position> positions = new ArrayList<>();
    private final Set<String> settlementReceipts = new HashSet<>();
    private final Set<String> marginWithdrawalReceipts = new HashSet<>();
    private final Map<String, Long> marginWithdrawalStarts = new HashMap<>();

    private record State(
            Map<String, Long> futuresPrice,
            Map<String, Long> netVolume,
            Map<String, Long> expiryDay,
            long dayCounter,
            Map<String, Long> marginBalance,
            List<Position> positions,
            long lastSettlementDay,
            List<String> settlementReceipts,
            List<String> marginWithdrawalReceipts,
            Map<String, Long> marginWithdrawalStarts) {
        static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("futuresPrice").forGetter(State::futuresPrice),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("netVolume").forGetter(State::netVolume),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("expiryDay").forGetter(State::expiryDay),
                Codec.LONG.fieldOf("dayCounter").forGetter(State::dayCounter),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("marginBalance").forGetter(State::marginBalance),
                Position.CODEC.listOf().fieldOf("positions").forGetter(State::positions),
                Codec.LONG.optionalFieldOf("lastSettlementDay", -1L).forGetter(State::lastSettlementDay),
                Codec.STRING.listOf().optionalFieldOf("settlementReceipts", List.of()).forGetter(State::settlementReceipts),
                Codec.STRING.listOf().optionalFieldOf("marginWithdrawalReceipts", List.of()).forGetter(State::marginWithdrawalReceipts),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("marginWithdrawalStarts", Map.of()).forGetter(State::marginWithdrawalStarts)
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
        setDirty();
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
        setDirty();
    }

    // ---- volume ----

    public long netVolume(String itemId) {
        return netVolume.getOrDefault(itemId, 0L);
    }

    public void addNetVolume(String itemId, long delta) {
        netVolume.merge(itemId, delta, Long::sum);
        setDirty();
    }

    public void resetNetVolume(String itemId) {
        netVolume.put(itemId, 0L);
        setDirty();
    }

    // ---- expiry & day counter ----

    public long expiryDay(String itemId) {
        return expiryDay.getOrDefault(itemId, 0L);
    }

    public void setExpiryDay(String itemId, long day) {
        expiryDay.put(itemId, day);
        setDirty();
    }

    public long dayCounter() {
        return dayCounter;
    }

    public long lastSettlementDay() {
        return lastSettlementDay;
    }

    public void advanceToSettlementDay(long settlementDay) {
        long target = settlementDay >= Long.MAX_VALUE - 1L ? Long.MAX_VALUE : Math.max(0L, settlementDay + 1L);
        if (target > dayCounter) {
            dayCounter = target;
            setDirty();
        }
    }

    public void markSettlementDay(long settlementDay) {
        if (settlementDay > lastSettlementDay) {
            lastSettlementDay = settlementDay;
            setDirty();
        }
    }

    public boolean recordSettlementOnce(String sourceId) {
        if (sourceId == null || sourceId.isBlank() || !settlementReceipts.add(sourceId)) return false;
        while (settlementReceipts.size() > 16384) settlementReceipts.remove(settlementReceipts.iterator().next());
        setDirty();
        return true;
    }

    public boolean hasMarginWithdrawal(String sourceId) {
        return sourceId != null && !sourceId.isBlank() && marginWithdrawalReceipts.contains(sourceId);
    }

    public boolean recordMarginWithdrawal(String sourceId) {
        if (sourceId == null || sourceId.isBlank() || !marginWithdrawalReceipts.add(sourceId)) return false;
        while (marginWithdrawalReceipts.size() > 8192) marginWithdrawalReceipts.remove(marginWithdrawalReceipts.iterator().next());
        setDirty();
        return true;
    }

    /** Persists the pre-debit balance before a margin withdrawal is applied. */
    public boolean recordMarginWithdrawalStart(String sourceId, long balanceBefore) {
        if (sourceId == null || sourceId.isBlank() || balanceBefore < 0L
                || marginWithdrawalStarts.containsKey(sourceId)) return false;
        marginWithdrawalStarts.put(sourceId, balanceBefore);
        while (marginWithdrawalStarts.size() > 8192) {
            marginWithdrawalStarts.remove(marginWithdrawalStarts.keySet().iterator().next());
        }
        setDirty();
        return true;
    }

    public Long marginWithdrawalStart(String sourceId) {
        return sourceId == null || sourceId.isBlank() ? null : marginWithdrawalStarts.get(sourceId);
    }

    public void clearMarginWithdrawalStart(String sourceId) {
        if (sourceId != null && marginWithdrawalStarts.remove(sourceId) != null) setDirty();
    }

    public void incrementDay() {
        if (dayCounter < Long.MAX_VALUE) dayCounter++;
        setDirty();
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
        setDirty();
    }

    public void addMarginBalance(UUID playerId, long delta) {
        long current = marginBalance(playerId);
        long next;
        if (delta > 0 && current > Long.MAX_VALUE - delta) {
            next = Long.MAX_VALUE;
        } else if (delta < 0 && current < Long.MIN_VALUE - delta) {
            next = 0;
        } else {
            next = current + delta;
        }
        setMarginBalance(playerId, next);
    }

    // ---- positions ----

    public List<Position> positions() {
        return positions;
    }

    public void addPosition(Position position) {
        positions.add(position);
        setDirty();
    }

    public void removePosition(String positionId) {
        positions.removeIf(position -> position.id().equals(positionId));
        setDirty();
    }

    public void replacePosition(Position position) {
        for (int i = 0; i < positions.size(); i++) {
            if (positions.get(i).id().equals(position.id())) {
                positions.set(i, position);
                setDirty();
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
                dayCounter, new HashMap<>(marginBalance), new ArrayList<>(positions), lastSettlementDay,
                new ArrayList<>(settlementReceipts), new ArrayList<>(marginWithdrawalReceipts),
                new HashMap<>(marginWithdrawalStarts));
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
                data.lastSettlementDay = state.lastSettlementDay();
                data.settlementReceipts.addAll(state.settlementReceipts());
                while (data.settlementReceipts.size() > 16384) data.settlementReceipts.remove(data.settlementReceipts.iterator().next());
                data.marginWithdrawalReceipts.addAll(state.marginWithdrawalReceipts());
                while (data.marginWithdrawalReceipts.size() > 8192) data.marginWithdrawalReceipts.remove(data.marginWithdrawalReceipts.iterator().next());
                data.marginWithdrawalStarts.putAll(state.marginWithdrawalStarts());
                while (data.marginWithdrawalStarts.size() > 8192) data.marginWithdrawalStarts.remove(data.marginWithdrawalStarts.keySet().iterator().next());
            });
        }
        return data;
    }
}
