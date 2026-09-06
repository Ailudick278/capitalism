package com.ailudick.capitalismmod.government;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent daily fixed-basket consumer price index history. */
public final class InflationSavedData extends SavedData {
    private static final String ID = "capitalismmod_inflation";
    private static final int MAX_SNAPSHOTS = 1024;
    private final List<Snapshot> snapshots = new ArrayList<>();

    public record Snapshot(long day, int indexBps, int dailyChangeBps) {}

    private InflationSavedData() {}

    public static InflationSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(InflationSavedData::new, InflationSavedData::load), ID);
    }

    public List<Snapshot> snapshots() { return List.copyOf(snapshots); }
    public Snapshot latest() { return snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1); }

    public Snapshot atOrBefore(long day) {
        for (int i = snapshots.size() - 1; i >= 0; i--) {
            if (snapshots.get(i).day() <= day) return snapshots.get(i);
        }
        return null;
    }

    public void record(Snapshot snapshot) {
        if (snapshot == null || (!snapshots.isEmpty() && snapshots.get(snapshots.size() - 1).day() >= snapshot.day())) return;
        snapshots.add(snapshot);
        while (snapshots.size() > MAX_SNAPSHOTS) snapshots.remove(0);
        setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Snapshot snapshot : snapshots) {
            CompoundTag value = new CompoundTag();
            value.putLong("day", snapshot.day());
            value.putInt("indexBps", snapshot.indexBps());
            value.putInt("dailyChangeBps", snapshot.dailyChangeBps());
            list.add(value);
        }
        tag.put("snapshots", list);
        return tag;
    }

    public static InflationSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        InflationSavedData data = new InflationSavedData();
        ListTag list = tag.getList("snapshots", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_SNAPSHOTS); i < list.size(); i++) {
            CompoundTag value = list.getCompound(i);
            data.snapshots.add(new Snapshot(Math.max(0L, value.getLong("day")),
                    Math.max(1, value.getInt("indexBps")), value.getInt("dailyChangeBps")));
        }
        return data;
    }
}
