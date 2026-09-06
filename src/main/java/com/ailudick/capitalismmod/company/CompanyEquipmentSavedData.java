package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.util.EconomyMath;
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

    public record Equipment(String machineType, int count, int condition, int usageRemainder) {
        private static final Codec<Equipment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("machineType").forGetter(Equipment::machineType),
                Codec.INT.fieldOf("count").forGetter(Equipment::count),
                Codec.INT.fieldOf("condition").forGetter(Equipment::condition),
                Codec.INT.optionalFieldOf("usageRemainder", 0).forGetter(Equipment::usageRemainder)
        ).apply(instance, Equipment::new));

        public Equipment(String machineType, int count, int condition) {
            this(machineType, count, condition, 0);
        }

        public Equipment {
            count = Math.max(0, count);
            condition = Math.max(0, Math.min(100, condition));
            usageRemainder = count <= 0 ? 0 : Math.max(0, Math.min(count - 1, usageRemainder));
        }
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

    /** Returns the carrying value of a selected number of machines at current condition. */
    public long bookValue(String companyId, MachineType type, int count) {
        if (type == null || type == MachineType.NONE || count <= 0) return 0L;
        Equipment current = get(companyId, type);
        if (current == null || current.count() < count || current.condition() <= 0) return 0L;
        long gross = EconomyMath.multiply(Math.max(0L, type.purchasePrice()), count);
        return gross < 0L ? Long.MAX_VALUE : carryingValue(gross, current.condition());
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

    /** Records one batch of group usage and applies wear after every machine has served once. */
    public boolean use(String companyId, MachineType type) {
        return useAndMeasureBookValueLoss(companyId, type) >= 0L;
    }

    /**
     * Applies one batch of group usage and returns any resulting book-value
     * loss. A group of N identical machines is worn by one condition point
     * after N batches, preventing parallel capacity from multiplying wear.
     */
    public long useAndMeasureBookValueLoss(String companyId, MachineType type) {
        if (type == null || type == MachineType.NONE) return 0L;
        Map<String, Equipment> company = new HashMap<>(equipment.getOrDefault(companyId, Map.of()));
        Equipment current = company.get(type.id());
        if (current == null || current.count() <= 0 || current.condition() <= 0) return -1L;
        long gross = EconomyMath.multiply(Math.max(0L, type.purchasePrice()), current.count());
        if (gross < 0L) gross = Long.MAX_VALUE;
        long before = carryingValue(gross, current.condition());
        int uses = current.usageRemainder() + 1;
        boolean completeUnitOfGroupUse = uses >= current.count();
        int nextRemainder = completeUnitOfGroupUse ? 0 : uses;
        int nextCondition = completeUnitOfGroupUse
                ? Math.max(0, current.condition() - 1) : current.condition();
        long loss = completeUnitOfGroupUse ? Math.max(0L, before - carryingValue(gross, nextCondition)) : 0L;
        company.put(type.id(), new Equipment(type.id(), current.count(), nextCondition, nextRemainder));
        equipment.put(companyId, company);
        setDirty();
        return loss;
    }

    private static long carryingValue(long gross, int condition) {
        if (gross <= 0L || condition <= 0) return 0L;
        if (gross == Long.MAX_VALUE) return Long.MAX_VALUE;
        int normalized = Math.min(100, condition);
        long whole = EconomyMath.multiply(gross / 100L, normalized);
        long remainder = gross % 100L * normalized / 100L;
        long value = EconomyMath.add(whole, remainder);
        return value < 0L ? Long.MAX_VALUE : value;
    }

    public boolean restore(String companyId, MachineType type, int targetCondition) {
        if (type == null || type == MachineType.NONE) return false;
        Map<String, Equipment> company = new HashMap<>(equipment.getOrDefault(companyId, Map.of()));
        Equipment current = company.get(type.id());
        if (current == null || current.count() <= 0) return false;
        int restored = Math.max(0, Math.min(100, targetCondition));
        company.put(type.id(), new Equipment(type.id(), current.count(), restored, 0));
        equipment.put(companyId, company);
        setDirty();
        return true;
    }

    public Map<String, Equipment> all(String companyId) {
        return Map.copyOf(equipment.getOrDefault(companyId, Map.of()));
    }

    /** Transfers installed equipment during a legal company merger. */
    public void transferCompany(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.equals(targetId)) return;
        Map<String, Equipment> source = equipment.getOrDefault(sourceId, Map.of());
        Map<String, Equipment> target = new HashMap<>(equipment.getOrDefault(targetId, Map.of()));
        for (Equipment incoming : source.values()) {
            Equipment existing = target.get(incoming.machineType());
            long combined = (long) (existing == null ? 0 : existing.count()) + Math.max(0, incoming.count());
            int count = (int) Math.min(Integer.MAX_VALUE, combined);
            int condition = existing == null ? incoming.condition()
                    : Math.min(existing.condition(), incoming.condition());
            int remainder = count <= 0 ? 0 : Math.min(count - 1,
                    Math.max(existing == null ? 0 : existing.usageRemainder(), incoming.usageRemainder()));
            target.put(incoming.machineType(), new Equipment(incoming.machineType(), count, condition, remainder));
        }
        equipment.remove(sourceId);
        if (target.isEmpty()) equipment.remove(targetId);
        else equipment.put(targetId, target);
        setDirty();
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
