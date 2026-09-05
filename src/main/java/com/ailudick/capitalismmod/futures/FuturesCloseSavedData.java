package com.ailudick.capitalismmod.futures;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Durable idempotency ledger for manually closed futures positions. */
public final class FuturesCloseSavedData extends SavedData {
    private static final String ID = "capitalismmod_futures_closes";
    private static final int MAX_RECORDS = 8192;
    private final Set<String> closed = new HashSet<>();

    private FuturesCloseSavedData() {
    }

    public static FuturesCloseSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(FuturesCloseSavedData::new, FuturesCloseSavedData::load), ID);
    }

    public boolean has(String positionId) {
        return positionId != null && !positionId.isBlank() && closed.contains(positionId);
    }

    public void record(String positionId) {
        if (positionId == null || positionId.isBlank() || !closed.add(positionId)) return;
        while (closed.size() > MAX_RECORDS) closed.remove(closed.iterator().next());
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        closed.forEach(id -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            list.add(entry);
        });
        tag.put("closed", list);
        return tag;
    }

    public static FuturesCloseSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FuturesCloseSavedData data = new FuturesCloseSavedData();
        ListTag list = tag.getList("closed", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            String id = list.getCompound(i).getString("id");
            if (!id.isBlank()) data.closed.add(id);
        }
        while (data.closed.size() > MAX_RECORDS) data.closed.remove(data.closed.iterator().next());
        return data;
    }
}
