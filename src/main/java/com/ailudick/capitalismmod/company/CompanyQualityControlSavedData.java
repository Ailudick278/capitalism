package com.ailudick.capitalismmod.company;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent screening and disposition records for production batches. */
public final class CompanyQualityControlSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_quality_control";
    private static final int MAX_RECORDS = 8192;
    private final List<Inspection> inspections = new ArrayList<>();

    public record Inspection(String batchId, String companyId, String status, int score,
                             String reason, long inspectedAt, long reviewedAt) {
        private static final Codec<Inspection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("batchId").forGetter(Inspection::batchId),
                Codec.STRING.fieldOf("companyId").forGetter(Inspection::companyId),
                Codec.STRING.fieldOf("status").forGetter(Inspection::status),
                Codec.INT.fieldOf("score").forGetter(Inspection::score),
                Codec.STRING.fieldOf("reason").forGetter(Inspection::reason),
                Codec.LONG.fieldOf("inspectedAt").forGetter(Inspection::inspectedAt),
                Codec.LONG.optionalFieldOf("reviewedAt", 0L).forGetter(Inspection::reviewedAt)
        ).apply(instance, Inspection::new));

        public Inspection {
            status = status == null || status.isBlank() ? "review" : status;
            score = Math.max(0, Math.min(100, score));
            reason = reason == null ? "" : reason;
            inspectedAt = Math.max(0L, inspectedAt);
            reviewedAt = Math.max(0L, reviewedAt);
        }
    }

    private record State(List<Inspection> inspections) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Inspection.CODEC.listOf().fieldOf("inspections").forGetter(State::inspections)
        ).apply(instance, State::new));
    }

    private CompanyQualityControlSavedData() {
    }

    public static CompanyQualityControlSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyQualityControlSavedData::new, CompanyQualityControlSavedData::load), ID);
    }

    /** Creates a non-regulatory process screening from the existing quality proxy. */
    public void screen(CompanyProductionBatchSavedData.Batch batch, long at) {
        if (batch == null || batch.id() == null || batch.id().isBlank()) return;
        if (find(batch.companyId(), batch.id()) != null) return;
        String status = batch.qualityScore() >= 80 ? "released"
                : batch.qualityScore() >= 60 ? "conditional" : "review";
        String reason = status.equals("released") ? "process_score_pass"
                : status.equals("conditional") ? "process_score_conditional" : "process_score_review";
        upsert(new Inspection(batch.id(), batch.companyId(), status, batch.qualityScore(), reason, at, 0L));
    }

    public Inspection find(String companyId, String batchId) {
        if (companyId == null || batchId == null) return null;
        for (int i = inspections.size() - 1; i >= 0; i--) {
            Inspection inspection = inspections.get(i);
            if (companyId.equals(inspection.companyId()) && batchId.equals(inspection.batchId())) return inspection;
        }
        return null;
    }

    public List<Inspection> forCompany(String companyId) {
        if (companyId == null || companyId.isBlank()) return List.of();
        List<Inspection> result = new ArrayList<>();
        for (int i = inspections.size() - 1; i >= 0; i--) {
            Inspection inspection = inspections.get(i);
            if (companyId.equals(inspection.companyId())) result.add(inspection);
        }
        return List.copyOf(result);
    }

    public boolean review(String companyId, String batchId, String status, long at) {
        Inspection previous = find(companyId, batchId);
        if (previous == null || !isManualStatus(status)) return false;
        upsert(new Inspection(previous.batchId(), previous.companyId(), status, previous.score(),
                "manual_" + status, previous.inspectedAt(), Math.max(0L, at)));
        return true;
    }

    public void mergeCompany(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.equals(targetId)) return;
        boolean changed = false;
        for (int i = 0; i < inspections.size(); i++) {
            Inspection inspection = inspections.get(i);
            if (sourceId.equals(inspection.companyId())) {
                inspections.set(i, new Inspection(inspection.batchId(), targetId, inspection.status(),
                        inspection.score(), inspection.reason(), inspection.inspectedAt(), inspection.reviewedAt()));
                changed = true;
            }
        }
        if (changed) setDirty();
    }

    private static boolean isManualStatus(String status) {
        return "released".equalsIgnoreCase(status) || "rework".equalsIgnoreCase(status)
                || "rejected".equalsIgnoreCase(status);
    }

    private void upsert(Inspection inspection) {
        for (int i = inspections.size() - 1; i >= 0; i--) {
            Inspection existing = inspections.get(i);
            if (existing.companyId().equals(inspection.companyId())
                    && existing.batchId().equals(inspection.batchId())) {
                inspections.set(i, inspection);
                setDirty();
                return;
            }
        }
        inspections.add(inspection);
        while (inspections.size() > MAX_RECORDS) inspections.remove(0);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(List.copyOf(inspections))).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyQualityControlSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyQualityControlSavedData data = new CompanyQualityControlSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result().ifPresent(state -> {
                data.inspections.addAll(state.inspections());
                while (data.inspections.size() > MAX_RECORDS) data.inspections.remove(0);
            });
        }
        return data;
    }
}
