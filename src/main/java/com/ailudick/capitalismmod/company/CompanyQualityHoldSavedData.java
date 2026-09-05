package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.Config;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Inventory quantities held back from sale or production while quality is unresolved. */
public final class CompanyQualityHoldSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_quality_holds";
    private static final int MAX_RECORDS = 8192;
    private final List<Hold> holds = new ArrayList<>();

    public record Hold(String batchId, String companyId, String itemId, int quantity) {
        private static final Codec<Hold> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("batchId").forGetter(Hold::batchId),
                Codec.STRING.fieldOf("companyId").forGetter(Hold::companyId),
                Codec.STRING.fieldOf("itemId").forGetter(Hold::itemId),
                Codec.INT.fieldOf("quantity").forGetter(Hold::quantity)
        ).apply(instance, Hold::new));

        public Hold {
            quantity = Math.max(0, quantity);
        }
    }

    private record State(List<Hold> holds) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Hold.CODEC.listOf().fieldOf("holds").forGetter(State::holds)
        ).apply(instance, State::new));
    }

    private CompanyQualityHoldSavedData() {
    }

    public static CompanyQualityHoldSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyQualityHoldSavedData::new, CompanyQualityHoldSavedData::load), ID);
    }

    public List<Hold> holds() {
        return List.copyOf(holds);
    }

    public int heldUnits(String companyId, String itemId) {
        if (companyId == null || itemId == null) return 0;
        long total = 0L;
        for (Hold hold : holds) {
            if (companyId.equals(hold.companyId()) && itemId.equals(hold.itemId())) {
                total = Math.min(Integer.MAX_VALUE, total + Math.max(0, hold.quantity()));
            }
        }
        return (int) total;
    }

    public int availableUnits(String companyId, String itemId, int warehouseUnits) {
        return Math.max(0, warehouseUnits - heldUnits(companyId, itemId));
    }

    public void hold(CompanyProductionBatchSavedData.Batch batch) {
        if (batch == null || batch.qualityScore() >= Config.COMPANY_QUALITY_RELEASE_THRESHOLD.get()) return;
        releaseBatch(batch.companyId(), batch.id());
        for (var output : batch.outputs().entrySet()) {
            if (output.getKey() != null && output.getValue() != null && output.getValue() > 0) {
                holds.add(new Hold(batch.id(), batch.companyId(), output.getKey(), output.getValue()));
            }
        }
        while (holds.size() > MAX_RECORDS) holds.remove(0);
        setDirty();
    }

    public boolean releaseBatch(String companyId, String batchId) {
        if (companyId == null || batchId == null) return false;
        boolean changed = holds.removeIf(hold -> companyId.equals(hold.companyId()) && batchId.equals(hold.batchId()));
        if (changed) setDirty();
        return changed;
    }

    public void mergeCompany(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.equals(targetId)) return;
        boolean changed = false;
        for (int i = 0; i < holds.size(); i++) {
            Hold hold = holds.get(i);
            if (sourceId.equals(hold.companyId())) {
                holds.set(i, new Hold(hold.batchId(), targetId, hold.itemId(), hold.quantity()));
                changed = true;
            }
        }
        if (changed) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(List.copyOf(holds))).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyQualityHoldSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyQualityHoldSavedData data = new CompanyQualityHoldSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result().ifPresent(state -> {
                data.holds.addAll(state.holds());
                while (data.holds.size() > MAX_RECORDS) data.holds.remove(0);
            });
        }
        return data;
    }
}
