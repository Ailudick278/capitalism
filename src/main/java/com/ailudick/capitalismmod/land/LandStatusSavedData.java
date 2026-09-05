package com.ailudick.capitalismmod.land;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Persists the last observed lifecycle state so transitions are announced once. */
public final class LandStatusSavedData extends SavedData {
    private static final String ID = "capitalismmod_land_status";
    private final Map<String, String> states = new HashMap<>();
    private LandStatusSavedData() {}

    public static LandStatusSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LandStatusSavedData::new, LandStatusSavedData::load), ID);
    }

    public String get(String claimId) { return states.get(claimId); }
    public void put(String claimId, LandStatus status) { states.put(claimId, status.name()); setDirty(); }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag values = new CompoundTag();
        states.forEach(values::putString);
        tag.put("states", values);
        return tag;
    }

    public static LandStatusSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandStatusSavedData data = new LandStatusSavedData();
        CompoundTag values = tag.getCompound("states");
        for (String key : values.getAllKeys()) data.states.put(key, values.getString(key));
        return data;
    }
}
