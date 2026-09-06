package com.ailudick.capitalismmod.government;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent audit of public-facility maintenance failures and depreciation. */
public final class PublicMaintenanceSavedData extends SavedData {
    private static final String ID = "capitalismmod_public_maintenance";
    private static final int MAX_RECORDS = 8192;
    private final List<Cut> cuts = new ArrayList<>();
    public record Cut(long day, String region, String facility, String reason) {}
    private PublicMaintenanceSavedData() {}
    public static PublicMaintenanceSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(PublicMaintenanceSavedData::new, PublicMaintenanceSavedData::load), ID);
    }
    public List<Cut> cuts() { return List.copyOf(cuts); }
    public boolean record(Cut cut) {
        if (cut == null || cut.day() < 0 || cut.region().isBlank() || cut.facility().isBlank()
                || cut.reason().isBlank() || cuts.stream().anyMatch(c -> c.day() == cut.day()
                && c.region().equals(cut.region()) && c.facility().equals(cut.facility()))) return false;
        cuts.add(cut); while (cuts.size() > MAX_RECORDS) cuts.remove(0); setDirty(); return true;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Cut c : cuts) { CompoundTag e = new CompoundTag(); e.putLong("day", c.day());
            e.putString("region", c.region()); e.putString("facility", c.facility());
            e.putString("reason", c.reason()); list.add(e); }
        tag.put("cuts", list); return tag;
    }
    public static PublicMaintenanceSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PublicMaintenanceSavedData data = new PublicMaintenanceSavedData();
        ListTag list = tag.getList("cuts", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_RECORDS); i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            if (e.getLong("day") >= 0 && !e.getString("region").isBlank()
                    && !e.getString("facility").isBlank() && !e.getString("reason").isBlank())
                data.cuts.add(new Cut(e.getLong("day"), e.getString("region"), e.getString("facility"), e.getString("reason")));
        }
        return data;
    }
}
