package com.ailudick.capitalismmod.government;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Durable unit-level inspection and labor-day records for public construction. */
public final class PublicConstructionInspectionSavedData extends SavedData {
    private static final String ID = "capitalismmod_public_construction_inspections";
    private static final int MAX_RECORDS = 8192;
    private final List<Inspection> inspections = new ArrayList<>();

    public record Inspection(String projectId, int unit, long workerDays, int averageSkill,
                             int qualityScore, String status, long day) {}

    private PublicConstructionInspectionSavedData() {}

    public static PublicConstructionInspectionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(PublicConstructionInspectionSavedData::new, PublicConstructionInspectionSavedData::load), ID);
    }

    public List<Inspection> inspections() { return List.copyOf(inspections); }

    public List<Inspection> pendingReworkBefore(long day) {
        return inspections.stream().filter(i -> "REWORK_REQUIRED".equals(i.status()) && i.day() < day).toList();
    }

    /** Reduces effective regional capacity for units that failed inspection and await rework. */
    public int effectiveUnits(MinecraftServer server, String region, String facility, int rawUnits) {
        if (rawUnits <= 0) return 0;
        var projects = PublicConstructionSavedData.get(server).projects();
        long failed = projects.stream().filter(p -> p.region().equals(region) && p.facility().equals(facility))
                .flatMap(p -> inspections.stream().filter(i -> i.projectId().equals(p.id())
                        && "REWORK_REQUIRED".equals(i.status())))
                .count();
        return (int) Math.max(0L, rawUnits - Math.min((long) rawUnits, failed));
    }

    public boolean markReworkCompleted(String projectId, int unit, long day) {
        for (int i = 0; i < inspections.size(); i++) {
            Inspection current = inspections.get(i);
            if (current.projectId().equals(projectId) && current.unit() == unit
                    && "REWORK_REQUIRED".equals(current.status())) {
                inspections.set(i, new Inspection(current.projectId(), current.unit(), current.workerDays(),
                        current.averageSkill(), current.qualityScore(), "REWORK_COMPLETED", day));
                setDirty();
                return true;
            }
        }
        return false;
    }

    public boolean record(Inspection inspection) {
        if (inspection == null || inspection.projectId().isBlank() || inspection.unit() <= 0
                || inspection.workerDays() <= 0L || inspection.averageSkill() < 0
                || inspection.averageSkill() > 100 || inspection.qualityScore() < 0
                || inspection.qualityScore() > 100 || inspection.status().isBlank()
                || inspections.stream().anyMatch(existing -> existing.projectId().equals(inspection.projectId())
                        && existing.unit() == inspection.unit())) return false;
        inspections.add(inspection);
        while (inspections.size() > MAX_RECORDS) inspections.remove(0);
        setDirty();
        return true;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Inspection inspection : inspections) {
            CompoundTag entry = new CompoundTag(); entry.putString("project", inspection.projectId());
            entry.putInt("unit", inspection.unit()); entry.putLong("workerDays", inspection.workerDays());
            entry.putInt("skill", inspection.averageSkill()); entry.putInt("quality", inspection.qualityScore());
            entry.putString("status", inspection.status()); entry.putLong("day", inspection.day()); list.add(entry);
        }
        tag.put("inspections", list); return tag;
    }

    public static PublicConstructionInspectionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PublicConstructionInspectionSavedData data = new PublicConstructionInspectionSavedData();
        ListTag list = tag.getList("inspections", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, list.size() - MAX_RECORDS); i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String project = entry.getString("project"), status = entry.getString("status");
            int unit = entry.getInt("unit"), skill = entry.getInt("skill"), quality = entry.getInt("quality");
            long workerDays = entry.getLong("workerDays");
            if (!project.isBlank() && !status.isBlank() && unit > 0 && workerDays > 0L
                    && skill >= 0 && skill <= 100 && quality >= 0 && quality <= 100) {
                data.inspections.add(new Inspection(project, unit, workerDays, skill, quality, status,
                        entry.getLong("day")));
            }
        }
        return data;
    }
}
