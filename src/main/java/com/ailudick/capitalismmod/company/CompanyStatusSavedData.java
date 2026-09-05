package com.ailudick.capitalismmod.company;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Persists the legal operating status of companies without changing old Company records. */
public final class CompanyStatusSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_status";
    private final Map<String, Status> statuses = new HashMap<>();

    public record Status(String companyId, String status, long changedAt, String reason) {
    }

    private CompanyStatusSavedData() {
    }

    public static CompanyStatusSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyStatusSavedData::new, CompanyStatusSavedData::load), ID);
    }

    public String statusOf(String companyId) {
        Status status = statuses.get(companyId);
        return status == null ? "ACTIVE" : status.status();
    }

    public Status record(String companyId) {
        return statuses.get(companyId);
    }

    public void set(Status status) {
        if (status == null || status.companyId() == null || status.companyId().isBlank()) return;
        statuses.put(status.companyId(), status);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Status status : statuses.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("companyId", status.companyId());
            entry.putString("status", status.status());
            entry.putLong("changedAt", status.changedAt());
            entry.putString("reason", status.reason() == null ? "" : status.reason());
            list.add(entry);
        }
        tag.put("statuses", list);
        return tag;
    }

    public static CompanyStatusSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyStatusSavedData data = new CompanyStatusSavedData();
        ListTag list = tag.getList("statuses", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.getString("companyId").isBlank() && !entry.getString("status").isBlank()) {
                data.statuses.put(entry.getString("companyId"), new Status(entry.getString("companyId"),
                        entry.getString("status"), Math.max(0L, entry.getLong("changedAt")), entry.getString("reason")));
            }
        }
        return data;
    }
}
