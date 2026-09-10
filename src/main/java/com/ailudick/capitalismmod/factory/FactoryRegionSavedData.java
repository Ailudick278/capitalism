package com.ailudick.capitalismmod.factory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent physical factory areas. Coordinates are inclusive block bounds. */
public final class FactoryRegionSavedData extends SavedData {
    private static final String ID = "capitalismmod_factory_regions";
    private final List<Region> regions = new ArrayList<>();

    public record Region(String id, String companyId, String dimension,
                         int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public Region {
            id = id == null ? "" : id.trim();
            companyId = companyId == null ? "" : companyId.trim();
            dimension = dimension == null ? "" : dimension.trim();
            if (minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("Invalid factory bounds");
        }
        public boolean contains(String levelDimension, int x, int y, int z) {
            return dimension.equals(levelDimension) && x >= minX && x <= maxX
                    && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
        public long volume() {
            return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }
    }

    private FactoryRegionSavedData() {}

    public static FactoryRegionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(FactoryRegionSavedData::new, FactoryRegionSavedData::load), ID);
    }

    public List<Region> regions() { return List.copyOf(regions); }

    public Region get(String id) {
        return regions.stream().filter(region -> region.id().equals(id)).findFirst().orElse(null);
    }

    public Region at(String dimension, int x, int y, int z) {
        return regions.stream().filter(region -> region.contains(dimension, x, y, z)).findFirst().orElse(null);
    }

    public boolean add(Region region) {
        if (region == null || region.id().isBlank() || region.companyId().isBlank()
                || region.dimension().isBlank() || region.volume() > 2_000_000L
                || get(region.id()) != null) return false;
        regions.add(region);
        setDirty();
        return true;
    }

    public boolean remove(String id) {
        boolean changed = regions.removeIf(region -> region.id().equals(id));
        if (changed) setDirty();
        return changed;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Region region : regions) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", region.id()); entry.putString("company", region.companyId());
            entry.putString("dimension", region.dimension());
            entry.putInt("minX", region.minX()); entry.putInt("minY", region.minY()); entry.putInt("minZ", region.minZ());
            entry.putInt("maxX", region.maxX()); entry.putInt("maxY", region.maxY()); entry.putInt("maxZ", region.maxZ());
            list.add(entry);
        }
        tag.put("regions", list);
        return tag;
    }

    public static FactoryRegionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FactoryRegionSavedData data = new FactoryRegionSavedData();
        ListTag list = tag.getList("regions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            try {
                Region region = new Region(entry.getString("id"), entry.getString("company"), entry.getString("dimension"),
                        entry.getInt("minX"), entry.getInt("minY"), entry.getInt("minZ"),
                        entry.getInt("maxX"), entry.getInt("maxY"), entry.getInt("maxZ"));
                if (!region.id().isBlank() && !region.companyId().isBlank() && !region.dimension().isBlank()
                        && region.volume() <= 2_000_000L) data.regions.add(region);
            } catch (IllegalArgumentException ignored) {}
        }
        return data;
    }

    public static String newId() { return "factory-" + UUID.randomUUID(); }
}
