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
    private long lastRecoveryObservationDay = -1L;
    private int recoveryStreak;
    private static final int REQUIRED_RECOVERY_DAYS = 3;

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
        active = true; startedDay = day; lastTransitionDay = day;
        recoveryStreak = 0; lastRecoveryObservationDay = -1L; setDirty();
    }

    public void recover(long day) {
        if (!active) return;
        active = false; lastTransitionDay = day;
        recoveryStreak = 0; lastRecoveryObservationDay = -1L; setDirty();
    }

    /** Records one daily recovery observation and releases the crisis after three consecutive days. */
    public boolean observeRecovery(long day) {
        if (!active || day == lastRecoveryObservationDay) return false;
        if (lastRecoveryObservationDay == day - 1L) recoveryStreak++;
        else recoveryStreak = 1;
        lastRecoveryObservationDay = day;
        setDirty();
        if (recoveryStreak < REQUIRED_RECOVERY_DAYS) return false;
        recover(day);
        return true;
    }

    public void resetRecoveryObservation() {
        if (!active || (recoveryStreak == 0 && lastRecoveryObservationDay < 0L)) return;
        recoveryStreak = 0;
        lastRecoveryObservationDay = -1L;
        setDirty();
    }

    public int recoveryStreak() { return recoveryStreak; }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("active", active); tag.putLong("startedDay", startedDay);
        tag.putLong("lastTransitionDay", lastTransitionDay);
        tag.putLong("lastRecoveryObservationDay", lastRecoveryObservationDay);
        tag.putInt("recoveryStreak", recoveryStreak);
        return tag;
    }

    public static FinancialCrisisSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FinancialCrisisSavedData data = new FinancialCrisisSavedData();
        data.active = tag.getBoolean("active");
        data.startedDay = tag.getLong("startedDay");
        data.lastTransitionDay = tag.getLong("lastTransitionDay");
        data.lastRecoveryObservationDay = tag.getLong("lastRecoveryObservationDay");
        data.recoveryStreak = Math.max(0, tag.getInt("recoveryStreak"));
        return data;
    }
}
