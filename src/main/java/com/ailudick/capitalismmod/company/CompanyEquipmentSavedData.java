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

/** Persistent installed industrial equipment owned by each company. */
public final class CompanyEquipmentSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_equipment";
    private final Map<String, Map<String, Equipment>> equipment = new HashMap<>();

    public record Equipment(String machineType, int count, int condition) {
        private static final Codec<Equipment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("machineType").forGetter(Equipment::machineType),
                Codec.INT.fieldOf("count").forGetter(Equipment::count),
                Codec.INT.fieldOf("condition").forGetter(Equipment::condition)
        ).apply(instance, Equipment::new));
    }

    private record State(Map<String, Map<String, Equipment>> equipment) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Equipment.CODEC))
                        .fieldOf("equipment").forGetter(State::equipment)
        ).apply(instance, State::new));
    }

    private CompanyEquipmentSavedData() {}

    public static CompanyEquipmentSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CompanyEquipmentSavedData::new, CompanyEquipmentSavedData::load), ID);
    }

    public Equipment get(String companyId, MachineType type) {
        return equipment.getOrDefault(companyId, Map.of()).get(type.id());
    }

    public int count(String companyId, MachineType type) {
        Equipment value = get(companyId, type);
        return value == null || value.condition() <= 0 ? 0 : value.count();
    }

    public void install(String companyId, MachineType type, int count) {
        if (type == null || type == MachineType.NONE || count <= 0) return;
        Map<String, Equipment> company = new HashMap<>(equipment.getOrDefault(companyId, Map.of()));
        Equipment current = company.get(type.id());
        long combined = (long) (current == null ? 0 : current.count()) + count;
        company.put(type.id(), new Equipment(type.id(), (int) Math.min(Integer.MAX_VALUE, combined), 100));
        equipment.put(companyId, company);
        setDirty();
    }

    public boolean remove(String companyId, MachineType type, int count) {
        if (type == null || count <= 0) return false;
        Map<String, Equipment> company = new HashMap<>(equipment.getOrDefault(companyId, Map.of()));
        Equipment current = company.get(type.id());
        if (current == null || current.count() < count) return false;
        int remaining = current.count() - count;
        if (remaining == 0) company.remove(type.id());
        else company.put(type.id(), new Equipment(type.id(), remaining, current.condition()));
        equipment.put(companyId, company);
        setDirty();
        return true;
    }

    public Map<String, Equipment> all(String companyId) {
        return Map.copyOf(equipment.getOrDefault(companyId, Map.of()));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        State.CODEC.encodeStart(NbtOps.INSTANCE, new State(equipment)).result()
                .ifPresent(encoded -> tag.put("data", encoded));
        return tag;
    }

    public static CompanyEquipmentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompanyEquipmentSavedData data = new CompanyEquipmentSavedData();
        if (tag.contains("data")) {
            State.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).result()
                    .ifPresent(state -> state.equipment().forEach((id, values) ->
                            data.equipment.put(id, new HashMap<>(values))));
        }
        return data;
    }
}
