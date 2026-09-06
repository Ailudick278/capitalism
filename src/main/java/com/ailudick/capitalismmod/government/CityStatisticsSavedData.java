package com.ailudick.capitalismmod.government;

import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import com.ailudick.capitalismmod.population.PopulationSavedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Bounded daily regional indicators for observing long-run economic feedback. */
public final class CityStatisticsSavedData extends SavedData {
    private static final String ID = "capitalismmod_city_statistics";
    private static final int MAX_SNAPSHOTS = 16384;
    private final List<Snapshot> snapshots = new ArrayList<>();

    public record Snapshot(long day, String region, int residents, int housing, int school, int clinic,
                           int serviceScore, long treasuryMinor, int activeProjects) {}

    private CityStatisticsSavedData() {}

    public static CityStatisticsSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CityStatisticsSavedData::new, CityStatisticsSavedData::load), ID);
    }

    public List<Snapshot> snapshots(String region, int days) {
        long latest = snapshots.stream().filter(s -> s.region().equals(region))
                .mapToLong(Snapshot::day).max().orElse(0L);
        return snapshots.stream().filter(s -> s.region().equals(region))
                .filter(s -> days <= 0 || s.day() >= Math.max(0L, latest - days + 1L)).toList();
    }

    public int recordDaily(MinecraftServer server, long day) {
        PopulationSavedData population = PopulationSavedData.get(server);
        LogisticsInfrastructureSavedData infrastructure = LogisticsInfrastructureSavedData.get(server);
        PublicConstructionSavedData construction = PublicConstructionSavedData.get(server);
        Set<String> regions = new HashSet<>(infrastructure.regions());
        population.households().forEach(h -> regions.add(h.region()));
        construction.projects().forEach(p -> regions.add(p.region()));
        int recorded = 0;
        for (String region : regions) {
            if (region == null || region.isBlank() || snapshots.stream().anyMatch(s -> s.day() == day && s.region().equals(region))) continue;
            int residents = population.population(region);
            int activeProjects = (int) construction.projects().stream()
                    .filter(p -> p.region().equals(region) && p.completedUnits() < p.units()).count();
            snapshots.add(new Snapshot(day, region, residents, infrastructure.count(region, "housing"),
                    infrastructure.count(region, "school"), infrastructure.count(region, "clinic"),
                    infrastructure.publicServiceScore(server, region, residents),
                    GovernmentPolicySavedData.get(server).treasuryMinor(), activeProjects));
            recorded++;
        }
        if (recorded > 0) {
            while (snapshots.size() > MAX_SNAPSHOTS) snapshots.remove(0);
            setDirty();
        }
        return recorded;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Snapshot s : snapshots) {
            CompoundTag e = new CompoundTag(); e.putLong("day", s.day()); e.putString("region", s.region());
            e.putInt("residents", s.residents()); e.putInt("housing", s.housing());
            e.putInt("school", s.school()); e.putInt("clinic", s.clinic()); e.putInt("score", s.serviceScore());
            e.putLong("treasury", s.treasuryMinor()); e.putInt("projects", s.activeProjects()); list.add(e);
        }
        tag.put("snapshots", list); return tag;
    }

    public static CityStatisticsSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CityStatisticsSavedData data = new CityStatisticsSavedData();
        ListTag list = tag.getList("snapshots", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_SNAPSHOTS); i < list.size(); i++) {
            CompoundTag e = list.getCompound(i); String region = e.getString("region");
            if (!region.isBlank() && e.getLong("day") >= 0L) data.snapshots.add(new Snapshot(e.getLong("day"), region,
                    Math.max(0, e.getInt("residents")), Math.max(0, e.getInt("housing")),
                    Math.max(0, e.getInt("school")), Math.max(0, e.getInt("clinic")),
                    Math.max(0, Math.min(100, e.getInt("score"))), Math.max(0L, e.getLong("treasury")),
                    Math.max(0, e.getInt("projects"))));
        }
        return data;
    }
}
