package com.ailudick.capitalismmod.market;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Persists logistics facilities grouped by trade region. */
public final class LogisticsInfrastructureSavedData extends SavedData {
    private static final String ID = "capitalismmod_logistics_infrastructure";
    private final Map<String, Map<String, Integer>> facilities = new HashMap<>();

    private record State(Map<String, Map<String, Integer>> facilities) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.INT))
                        .fieldOf("facilities").forGetter(State::facilities)
        ).apply(instance, State::new));
    }

    private LogisticsInfrastructureSavedData() {
    }

    public static LogisticsInfrastructureSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LogisticsInfrastructureSavedData::new, LogisticsInfrastructureSavedData::load), ID);
    }

    public void register(String region, String facility) {
        facilities.computeIfAbsent(region, key -> new HashMap<>()).merge(facility, 1, Integer::sum);
        setDirty();
    }

    public void unregister(String region, String facility) {
        Map<String, Integer> regionFacilities = facilities.get(region);
        if (regionFacilities == null) {
            return;
        }
        int next = regionFacilities.getOrDefault(facility, 0) - 1;
        if (next <= 0) {
            regionFacilities.remove(facility);
        } else {
            regionFacilities.put(facility, next);
        }
        if (regionFacilities.isEmpty()) {
            facilities.remove(region);
        }
        setDirty();
    }

    public int count(String region, String facility) {
        return facilities.getOrDefault(region, Map.of()).getOrDefault(facility, 0);
    }

    public int capacityBonus(String origin, String destination, TransportMode mode) {
        int bonus = 0;
        bonus += 128 * (count(origin, "logistics_center") + count(destination, "logistics_center"));
        bonus += 64 * (count(origin, "transfer_station") + count(destination, "transfer_station"));
        if (mode == TransportMode.SEA) {
            bonus += 512 * (count(origin, "port") + count(destination, "port"));
        }
        return Math.min(4096, bonus);
    }

    public long adjustTravelTicks(long ticks, String origin, String destination, TransportMode mode) {
        int reduction = 0;
        reduction += 10 * (count(origin, "logistics_center") + count(destination, "logistics_center"));
        reduction += 15 * (count(origin, "transfer_station") + count(destination, "transfer_station"));
        if (mode == TransportMode.SEA) {
            reduction += 25 * (count(origin, "port") + count(destination, "port"));
        }
        reduction = Math.min(70, reduction);
        return Math.max(1L, ticks * (100L - reduction) / 100L);
    }

    public double riskReduction(String origin, String destination, TransportMode mode) {
        double reduction = 0.05 * (count(origin, "logistics_center") + count(destination, "logistics_center"));
        reduction += 0.10 * (count(origin, "transfer_station") + count(destination, "transfer_station"));
        if (mode == TransportMode.SEA) {
            reduction += 0.15 * (count(origin, "port") + count(destination, "port"));
        }
        return Math.min(0.80, reduction);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(facilities)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static LogisticsInfrastructureSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LogisticsInfrastructureSavedData data = new LogisticsInfrastructureSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> state.facilities().forEach((region, values) ->
                            data.facilities.put(region, new HashMap<>(values))));
        }
        return data;
    }
}
