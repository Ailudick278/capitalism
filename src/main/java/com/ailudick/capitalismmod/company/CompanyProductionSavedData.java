package com.ailudick.capitalismmod.company;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/** Persists production clocks and counters so production is restart-safe and auditable. */
public final class CompanyProductionSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_production";
    private final Map<String, ProductionState> states = new HashMap<>();

    public record ProductionState(String companyId, long lastProcessedTick,
                                  long successfulCycles, long failedCycles,
                                  Map<String, Long> failureReasons) {
        public ProductionState(String companyId, long lastProcessedTick,
                               long successfulCycles, long failedCycles) {
            this(companyId, lastProcessedTick, successfulCycles, failedCycles, Map.of());
        }

        public ProductionState {
            failureReasons = failureReasons == null ? Map.of() : Map.copyOf(failureReasons);
        }

        private static final Codec<ProductionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("companyId").forGetter(ProductionState::companyId),
                Codec.LONG.fieldOf("lastProcessedTick").forGetter(ProductionState::lastProcessedTick),
                Codec.LONG.fieldOf("successfulCycles").forGetter(ProductionState::successfulCycles),
                Codec.LONG.fieldOf("failedCycles").forGetter(ProductionState::failedCycles),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("failureReasons", Map.of())
                        .forGetter(ProductionState::failureReasons)
        ).apply(instance, ProductionState::new));
    }

    private record State(Map<String, ProductionState> states) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, ProductionState.CODEC).fieldOf("states")
                        .forGetter(State::states)
        ).apply(instance, State::new));
    }

    private CompanyProductionSavedData() {}

    public static CompanyProductionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyProductionSavedData::new, CompanyProductionSavedData::load), ID);
    }

    public ProductionState get(String companyId) {
        return states.get(companyId);
    }

    public void put(ProductionState state) {
        if (state == null || state.companyId() == null || state.companyId().isBlank()) return;
        states.put(state.companyId(), state);
        setDirty();
    }

    public static Map<String, Long> incrementReason(Map<String, Long> reasons, String reason) {
        Map<String, Long> updated = new HashMap<>(reasons == null ? Map.of() : reasons);
        String key = reason == null || reason.isBlank() ? "unknown" : reason;
        long previous = Math.max(0L, updated.getOrDefault(key, 0L));
        updated.put(key, previous == Long.MAX_VALUE ? previous : previous + 1L);
        return updated;
    }

    public void remove(String companyId) {
        if (states.remove(companyId) != null) setDirty();
    }

    /** Combines production history when one company is absorbed by another. */
    public void mergeCompany(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.equals(targetId)) return;
        ProductionState source = states.remove(sourceId);
        ProductionState target = states.get(targetId);
        if (source == null) {
            if (target != null) setDirty();
            return;
        }
        if (target == null) {
            states.put(targetId, new ProductionState(targetId, source.lastProcessedTick(),
                    source.successfulCycles(), source.failedCycles(), source.failureReasons()));
        } else {
            Map<String, Long> reasons = new HashMap<>(target.failureReasons());
            source.failureReasons().forEach((key, value) -> reasons.merge(key, Math.max(0L, value),
                    (left, right) -> left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right));
            states.put(targetId, new ProductionState(targetId,
                    Math.min(source.lastProcessedTick(), target.lastProcessedTick()),
                    add(source.successfulCycles(), target.successfulCycles()),
                    add(source.failedCycles(), target.failedCycles()), reasons));
        }
        setDirty();
    }

    private static long add(long left, long right) {
        try { return Math.addExact(Math.max(0L, left), Math.max(0L, right)); }
        catch (ArithmeticException e) { return Long.MAX_VALUE; }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(states)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyProductionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyProductionSavedData data = new CompanyProductionSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> data.states.putAll(state.states()));
        }
        return data;
    }
}
