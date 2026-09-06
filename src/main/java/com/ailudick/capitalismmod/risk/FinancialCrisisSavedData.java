package com.ailudick.capitalismmod.risk;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent hysteresis state for a system-wide financial crisis. */
public final class FinancialCrisisSavedData extends SavedData {
    private static final String ID = "capitalismmod_financial_crisis";
    private boolean active;
    private long startedDay = -1L;
    private long lastTransitionDay = -1L;

    private FinancialCrisisSavedData() {}

    public static FinancialCrisisSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(FinancialCrisisSavedData::new, FinancialCrisisSavedData::load), ID);
    }

    public boolean active() { return active; }
    public long startedDay() { return startedDay; }
    public long lastTransitionDay() { return lastTransitionDay; }

    public void enter(long day) {
        if (active) return;
        active = true; startedDay = day; lastTransitionDay = day; setDirty();
    }

    public void recover(long day) {
        if (!active) return;
        active = false; lastTransitionDay = day; setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("active", active); tag.putLong("startedDay", startedDay);
        tag.putLong("lastTransitionDay", lastTransitionDay); return tag;
    }

    public static FinancialCrisisSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FinancialCrisisSavedData data = new FinancialCrisisSavedData();
        data.active = tag.getBoolean("active");
        data.startedDay = tag.getLong("startedDay");
        data.lastTransitionDay = tag.getLong("lastTransitionDay");
        return data;
    }
}
