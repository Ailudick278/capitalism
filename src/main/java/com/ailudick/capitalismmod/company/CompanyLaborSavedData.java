package com.ailudick.capitalismmod.company;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent labor contracts; contracts are intentionally independent of loaded NPC entities. */
public final class CompanyLaborSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_labor";
    private final Map<String, List<WorkerContract>> contracts = new HashMap<>();

    public record WorkerContract(String id, String companyId, String role, int count,
                                 long dailyWage, int skill, boolean active) {
        private static final Codec<WorkerContract> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(WorkerContract::id),
                Codec.STRING.fieldOf("companyId").forGetter(WorkerContract::companyId),
                Codec.STRING.fieldOf("role").forGetter(WorkerContract::role),
                Codec.INT.fieldOf("count").forGetter(WorkerContract::count),
                Codec.LONG.fieldOf("dailyWage").forGetter(WorkerContract::dailyWage),
                Codec.INT.fieldOf("skill").forGetter(WorkerContract::skill),
                Codec.BOOL.fieldOf("active").forGetter(WorkerContract::active)
        ).apply(instance, WorkerContract::new));
    }

    private record State(Map<String, List<WorkerContract>> contracts) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, WorkerContract.CODEC.listOf()).fieldOf("contracts")
                        .forGetter(State::contracts)
        ).apply(instance, State::new));
    }

    private CompanyLaborSavedData() {}

    public static CompanyLaborSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyLaborSavedData::new, CompanyLaborSavedData::load), ID);
    }

    public List<WorkerContract> contracts(String companyId) {
        return List.copyOf(contracts.getOrDefault(companyId, List.of()));
    }

    public void add(WorkerContract contract) {
        if (contract == null || contract.companyId().isBlank() || contract.count() <= 0
                || contract.dailyWage() < 0L) return;
        contracts.computeIfAbsent(contract.companyId(), ignored -> new ArrayList<>()).add(contract);
        setDirty();
    }

    public void remove(String companyId, String contractId) {
        List<WorkerContract> list = contracts.get(companyId);
        if (list != null && list.removeIf(contract -> contract.id().equals(contractId))) setDirty();
    }

    public int activeWorkers(String companyId) {
        long total = contracts(companyId).stream().filter(WorkerContract::active)
                .mapToLong(WorkerContract::count).sum();
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public long dailyWages(String companyId) {
        long total = 0L;
        for (WorkerContract contract : contracts(companyId)) {
            if (!contract.active()) continue;
            long cost;
            try {
                cost = Math.multiplyExact(contract.dailyWage(), contract.count());
                total = Math.addExact(total, cost);
            } catch (ArithmeticException e) {
                return Long.MAX_VALUE;
            }
        }
        return total;
    }

    public int averageSkill(String companyId) {
        long workers = 0L;
        long weighted = 0L;
        for (WorkerContract contract : contracts(companyId)) {
            if (!contract.active()) continue;
            workers += contract.count();
            weighted += (long) contract.count() * Math.max(0, contract.skill());
        }
        return workers <= 0L ? 0 : (int) Math.min(100L, weighted / workers);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(contracts)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyLaborSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyLaborSavedData data = new CompanyLaborSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> state.contracts().forEach((id, list) ->
                            data.contracts.put(id, new ArrayList<>(list))));
        }
        return data;
    }
}
