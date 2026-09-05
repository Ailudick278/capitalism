package com.ailudick.capitalismmod.company;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

/** Persistent finite petroleum reserves keyed by dimension and chunk. */
public final class OilFieldSavedData extends SavedData {
    private static final String ID = "capitalismmod_oil_fields";
    private static final int FIELD_CHANCE_PERCENT = 12;
    private static final long MIN_RESERVE = 1_200L;
    private static final long RESERVE_RANGE = 8_800L;
    private final Map<String, Field> fields = new HashMap<>();

    public record Field(String key, String dimension, int chunkX, int chunkZ,
                        long initialReserve, long remainingReserve) {}

    private OilFieldSavedData() {}

    public static OilFieldSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(OilFieldSavedData::new, OilFieldSavedData::load), ID);
    }

    public Field get(String dimension, int chunkX, int chunkZ) {
        return fields.get(key(dimension, chunkX, chunkZ));
    }

    public List<Field> fieldsInDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) return List.of();
        return fields.values().stream().filter(field -> dimension.equals(field.dimension())).toList();
    }

    /** Returns an existing field, or creates one only when deterministic prospecting finds it. */
    public Field prospect(String dimension, int chunkX, int chunkZ) {
        if (dimension == null || dimension.isBlank()) return null;
        String key = key(dimension, chunkX, chunkZ);
        Field existing = fields.get(key);
        if (existing != null) return existing;
        long hash = stableHash(key);
        if (Math.floorMod(hash, 100L) >= FIELD_CHANCE_PERCENT) return null;
        long reserve = MIN_RESERVE + Math.floorMod(hash >>> 8, RESERVE_RANGE);
        Field created = new Field(key, dimension, chunkX, chunkZ, reserve, reserve);
        fields.put(key, created);
        setDirty();
        return created;
    }

    public boolean canExtract(Field field, long amount) {
        return field != null && amount > 0L && field.remainingReserve() >= amount;
    }

    public boolean extract(Field field, long amount) {
        if (!canExtract(field, amount)) return false;
        fields.put(field.key(), new Field(field.key(), field.dimension(), field.chunkX(), field.chunkZ(),
                field.initialReserve(), field.remainingReserve() - amount));
        setDirty();
        return true;
    }

    private static String key(String dimension, int chunkX, int chunkZ) {
        return dimension + ":" + chunkX + ":" + chunkZ;
    }

    private static long stableHash(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).getMostSignificantBits()
                ^ UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).getLeastSignificantBits();
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Field field : fields.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("key", field.key());
            entry.putString("dimension", field.dimension());
            entry.putInt("x", field.chunkX());
            entry.putInt("z", field.chunkZ());
            entry.putLong("initial", field.initialReserve());
            entry.putLong("remaining", field.remainingReserve());
            list.add(entry);
        }
        tag.put("fields", list);
        return tag;
    }

    public static OilFieldSavedData load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        OilFieldSavedData data = new OilFieldSavedData();
        ListTag list = tag.getList("fields", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String dimension = entry.getString("dimension");
            String key = entry.getString("key");
            long initial = entry.getLong("initial");
            long remaining = entry.getLong("remaining");
            if (!dimension.isBlank() && !key.isBlank() && initial > 0L && remaining >= 0L) {
                data.fields.put(key, new Field(key, dimension, entry.getInt("x"), entry.getInt("z"),
                        initial, Math.min(initial, remaining)));
            }
        }
        return data;
    }
}
