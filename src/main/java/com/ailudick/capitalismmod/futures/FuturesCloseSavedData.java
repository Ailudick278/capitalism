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
    private final Set<String> marginCredited = new HashSet<>();
    private final Set<String> volumeAdjusted = new HashSet<>();

    private FuturesCloseSavedData() {
    }

    public static FuturesCloseSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(FuturesCloseSavedData::new, FuturesCloseSavedData::load), ID);
    }

    public boolean has(String positionId) {
        return positionId != null && !positionId.isBlank() && closed.contains(positionId);
    }

    public boolean hasMarginCredit(String positionId) {
        return positionId != null && !positionId.isBlank() && marginCredited.contains(positionId);
    }

    public boolean hasVolumeAdjustment(String positionId) {
        return positionId != null && !positionId.isBlank() && volumeAdjusted.contains(positionId);
    }

    public void recordMarginCredit(String positionId) {
        recordPhase(marginCredited, positionId);
    }

    public void recordVolumeAdjustment(String positionId) {
        recordPhase(volumeAdjusted, positionId);
    }

    public void record(String positionId) {
        recordPhase(closed, positionId);
    }

    private void recordPhase(Set<String> phase, String positionId) {
        if (positionId == null || positionId.isBlank() || !phase.add(positionId)) return;
        while (phase.size() > MAX_RECORDS) phase.remove(phase.iterator().next());
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
        putPhase(tag, "marginCredited", marginCredited);
        putPhase(tag, "volumeAdjusted", volumeAdjusted);
        return tag;
    }

    private static void putPhase(CompoundTag tag, String key, Set<String> values) {
        ListTag list = new ListTag();
        values.forEach(id -> { CompoundTag entry = new CompoundTag(); entry.putString("id", id); list.add(entry); });
        tag.put(key, list);
    }

    public static FuturesCloseSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FuturesCloseSavedData data = new FuturesCloseSavedData();
        ListTag list = tag.getList("closed", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            String id = list.getCompound(i).getString("id");
            if (!id.isBlank()) data.closed.add(id);
        }
        loadPhase(data.marginCredited, tag.getList("marginCredited", Tag.TAG_COMPOUND));
        loadPhase(data.volumeAdjusted, tag.getList("volumeAdjusted", Tag.TAG_COMPOUND));
        while (data.closed.size() > MAX_RECORDS) data.closed.remove(data.closed.iterator().next());
        while (data.marginCredited.size() > MAX_RECORDS) data.marginCredited.remove(data.marginCredited.iterator().next());
        while (data.volumeAdjusted.size() > MAX_RECORDS) data.volumeAdjusted.remove(data.volumeAdjusted.iterator().next());
        return data;
    }

    private static void loadPhase(Set<String> target, ListTag list) {
        for (int i = 0; i < list.size(); i++) {
            String id = list.getCompound(i).getString("id");
            if (!id.isBlank()) target.add(id);
        }
    }
}
