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
import java.util.Map;
import java.util.UUID;

/** Durable make records for production-batch traceability and later recalls. */
public final class CompanyProductionBatchSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_production_batches";
    private static final int MAX_RECORDS = 8192;
    private final List<Batch> batches = new ArrayList<>();

    public record Batch(String id, String companyId, String recipeId, String machineType,
                        Map<String, Integer> inputs, Map<String, Integer> outputs,
                        long conversionCost, int qualityScore, int workers, long createdAt,
                        String siteDimension, int siteChunkX, int siteChunkZ) {
        private static final Codec<Batch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Batch::id),
                Codec.STRING.fieldOf("companyId").forGetter(Batch::companyId),
                Codec.STRING.fieldOf("recipeId").forGetter(Batch::recipeId),
                Codec.STRING.fieldOf("machineType").forGetter(Batch::machineType),
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("inputs").forGetter(Batch::inputs),
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("outputs").forGetter(Batch::outputs),
                Codec.LONG.fieldOf("conversionCost").forGetter(Batch::conversionCost),
                Codec.INT.fieldOf("qualityScore").forGetter(Batch::qualityScore),
                Codec.INT.fieldOf("workers").forGetter(Batch::workers),
                Codec.LONG.fieldOf("createdAt").forGetter(Batch::createdAt),
                Codec.STRING.optionalFieldOf("siteDimension", "").forGetter(Batch::siteDimension),
                Codec.INT.optionalFieldOf("siteChunkX", 0).forGetter(Batch::siteChunkX),
                Codec.INT.optionalFieldOf("siteChunkZ", 0).forGetter(Batch::siteChunkZ)
        ).apply(instance, Batch::new));

        public Batch {
            inputs = inputs == null ? Map.of() : Map.copyOf(inputs);
            outputs = outputs == null ? Map.of() : Map.copyOf(outputs);
            conversionCost = Math.max(0L, conversionCost);
            qualityScore = Math.max(0, Math.min(100, qualityScore));
            workers = Math.max(0, workers);
            createdAt = Math.max(0L, createdAt);
            siteDimension = siteDimension == null ? "" : siteDimension;
        }

        /** Compatibility constructor for callers and older integrations. */
        public Batch(String id, String companyId, String recipeId, String machineType,
                     Map<String, Integer> inputs, Map<String, Integer> outputs,
                     long conversionCost, int qualityScore, int workers, long createdAt) {
            this(id, companyId, recipeId, machineType, inputs, outputs, conversionCost,
                    qualityScore, workers, createdAt, "", 0, 0);
        }
    }

    private record State(List<Batch> batches) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Batch.CODEC.listOf().fieldOf("batches").forGetter(State::batches)
        ).apply(instance, State::new));
    }

    private CompanyProductionBatchSavedData() {
    }

    public static CompanyProductionBatchSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyProductionBatchSavedData::new, CompanyProductionBatchSavedData::load), ID);
    }

    public void record(Batch batch) {
        if (batch == null || batch.id() == null || batch.id().isBlank()
                || batch.companyId() == null || batch.companyId().isBlank()
                || batch.outputs().isEmpty()) return;
        if (batches.stream().anyMatch(existing -> existing.id().equals(batch.id()))) return;
        batches.add(batch);
        while (batches.size() > MAX_RECORDS) batches.remove(0);
        setDirty();
    }

    public List<Batch> forCompany(String companyId) {
        if (companyId == null || companyId.isBlank()) return List.of();
        List<Batch> result = new ArrayList<>();
        for (int i = batches.size() - 1; i >= 0; i--) {
            Batch batch = batches.get(i);
            if (companyId.equals(batch.companyId())) result.add(batch);
        }
        return List.copyOf(result);
    }

    public Batch find(String companyId, String batchId) {
        if (companyId == null || batchId == null) return null;
        for (int i = batches.size() - 1; i >= 0; i--) {
            Batch batch = batches.get(i);
            if (companyId.equals(batch.companyId()) && batchId.equals(batch.id())) return batch;
        }
        return null;
    }

    /** Finds a batch by its globally unique audit ID, including after a retry. */
    public Batch findById(String batchId) {
        if (batchId == null || batchId.isBlank()) return null;
        for (int i = batches.size() - 1; i >= 0; i--) {
            if (batchId.equals(batches.get(i).id())) return batches.get(i);
        }
        return null;
    }

    public void mergeCompany(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.equals(targetId)) return;
        boolean changed = false;
        for (int i = 0; i < batches.size(); i++) {
            Batch batch = batches.get(i);
            if (sourceId.equals(batch.companyId())) {
                batches.set(i, new Batch(batch.id(), targetId, batch.recipeId(), batch.machineType(),
                        batch.inputs(), batch.outputs(), batch.conversionCost(), batch.qualityScore(),
                        batch.workers(), batch.createdAt(), batch.siteDimension(), batch.siteChunkX(),
                        batch.siteChunkZ()));
                changed = true;
            }
        }
        if (changed) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(List.copyOf(batches))).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyProductionBatchSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyProductionBatchSavedData data = new CompanyProductionBatchSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result().ifPresent(state -> {
                data.batches.addAll(state.batches());
                while (data.batches.size() > MAX_RECORDS) data.batches.remove(0);
            });
        }
        return data;
    }

    public static Batch newBatch(Company company, ProductionRecipe recipe, long conversionCost,
                                 int qualityScore, int workers, long createdAt) {
        return newBatch(company, recipe, conversionCost, qualityScore, workers, createdAt, null);
    }

    public static Batch newBatch(Company company, ProductionRecipe recipe, long conversionCost,
                                 int qualityScore, int workers, long createdAt,
                                 CompanySiteSavedData.Site site) {
        return newBatch(company, recipe, conversionCost, qualityScore, workers, createdAt, site, null);
    }

    /** Creates a batch with a stable audit ID when the caller has a durable cycle key. */
    public static Batch newBatch(Company company, ProductionRecipe recipe, long conversionCost,
                                 int qualityScore, int workers, long createdAt,
                                 CompanySiteSavedData.Site site, String stableId) {
        String dimension = site == null ? "" : site.dimension();
        int chunkX = site == null ? 0 : site.chunkX();
        int chunkZ = site == null ? 0 : site.chunkZ();
        String id = stableId == null || stableId.isBlank() ? UUID.randomUUID().toString() : stableId;
        return new Batch(id, company.companyId(), recipe.id(), recipe.machineType(),
                recipe.inputs(), recipe.outputs(), conversionCost, qualityScore, workers, createdAt,
                dimension, chunkX, chunkZ);
    }
}
