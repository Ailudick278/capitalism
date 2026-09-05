package com.ailudick.capitalismmod.land;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Durable journal for the multi-step settlement of a negotiated land transfer. */
public final class LandTransferSettlementSavedData extends SavedData {
    private static final String ID = "capitalismmod_land_transfer_settlements";
    private static final int MAX_RECORDS = 16384;
    private final Set<String> completed = new HashSet<>();

    private LandTransferSettlementSavedData() {
    }

    public static LandTransferSettlementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LandTransferSettlementSavedData::new, LandTransferSettlementSavedData::load), ID);
    }

    public boolean has(String key) {
        return key != null && !key.isBlank() && completed.contains(key);
    }

    public void record(String key) {
        if (key == null || key.isBlank() || !completed.add(key)) return;
        while (completed.size() > MAX_RECORDS) completed.remove(completed.iterator().next());
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        completed.forEach(key -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("key", key);
            list.add(entry);
        });
        tag.put("completed", list);
        return tag;
    }

    public static LandTransferSettlementSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LandTransferSettlementSavedData data = new LandTransferSettlementSavedData();
        ListTag list = tag.getList("completed", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            String key = list.getCompound(i).getString("key");
            if (!key.isBlank()) data.completed.add(key);
        }
        while (data.completed.size() > MAX_RECORDS) data.completed.remove(data.completed.iterator().next());
        return data;
    }
}
