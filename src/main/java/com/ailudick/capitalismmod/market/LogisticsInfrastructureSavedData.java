package com.ailudick.capitalismmod.market;

import com.ailudick.capitalismmod.population.MigrationEconomics;
import com.ailudick.capitalismmod.population.PublicServiceEconomics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    /** Regions with any persisted facility, used by regional public-budget passes. */
    public Set<String> regions() {
        return Set.copyOf(facilities.keySet());
    }

    public static boolean isPublicFacility(String facility) {
        return "housing".equals(facility) || "school".equals(facility) || "clinic".equals(facility);
    }

    public boolean changePublicFacility(String region, String facility, int delta) {
        if (region == null || region.isBlank() || !isPublicFacility(facility) || delta == 0) return false;
        int current = count(region, facility);
        long next = (long) current + delta;
        if (next < 0L || next > 1_000_000L) return false;
        if (delta > 0) {
            facilities.computeIfAbsent(region, key -> new HashMap<>()).put(facility, (int) next);
        } else if (next == 0L) {
            Map<String, Integer> values = facilities.get(region);
            if (values != null) values.remove(facility);
            if (values != null && values.isEmpty()) facilities.remove(region);
        } else {
            facilities.getOrDefault(region, Map.of()).put(facility, (int) next);
        }
        setDirty();
        return true;
    }

    /** Returns a 0-100 service score with a modest baseline for private provision. */
    public int publicServiceScore(String region, int residents) {
        return PublicServiceEconomics.score(residents, count(region, "housing"),
                count(region, "school"), count(region, "clinic"));
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

    /** A bounded connectivity score shared by regional economic systems. */
    public int accessScore(String region) {
        if (region == null || region.isBlank()) return 0;
        int score = 10 * count(region, "logistics_center")
                + 15 * count(region, "transfer_station")
                + 25 * count(region, "port");
        return Math.min(100, Math.max(0, score));
    }

    /** Returns the daily-need-equivalent friction of moving to a region. */
    public long migrationFriction(long dailyNeedMinor, int householdSize, String destination) {
        return migrationFrictionForAccess(dailyNeedMinor, householdSize, accessScore(destination));
    }

    /** Pure arithmetic form used by tests and by future regional service models. */
    public static long migrationFrictionForAccess(long dailyNeedMinor, int householdSize, int accessScore) {
        return MigrationEconomics.friction(dailyNeedMinor, householdSize, accessScore);
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
