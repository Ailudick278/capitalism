package com.ailudick.capitalismmod.economy;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Persists the last world day processed by the economy settlement service. */
public final class EconomySettlementSavedData extends SavedData {
    private static final String ID = "capitalismmod_economy_settlement";
    private static final String LAST_DAY = "lastSettlementDay";
    private long lastSettlementDay = -1;

    private EconomySettlementSavedData() {
    }

    public static EconomySettlementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(EconomySettlementSavedData::new, EconomySettlementSavedData::load), ID);
    }

    public long lastSettlementDay() {
        return lastSettlementDay;
    }

    public void setLastSettlementDay(long day) {
        lastSettlementDay = day;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong(LAST_DAY, lastSettlementDay);
        return tag;
    }

    public static EconomySettlementSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EconomySettlementSavedData data = new EconomySettlementSavedData();
        if (tag.contains(LAST_DAY)) {
            data.lastSettlementDay = tag.getLong(LAST_DAY);
        }
        return data;
    }
}
