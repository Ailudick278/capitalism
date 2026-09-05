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
                                  long successfulCycles, long failedCycles) {
        private static final Codec<ProductionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("companyId").forGetter(ProductionState::companyId),
                Codec.LONG.fieldOf("lastProcessedTick").forGetter(ProductionState::lastProcessedTick),
                Codec.LONG.fieldOf("successfulCycles").forGetter(ProductionState::successfulCycles),
                Codec.LONG.fieldOf("failedCycles").forGetter(ProductionState::failedCycles)
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

    public void remove(String companyId) {
        if (states.remove(companyId) != null) setDirty();
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
