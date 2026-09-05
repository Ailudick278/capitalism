package com.ailudick.capitalismmod.worldmap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Persistent server cache of rendered world-map chunk tiles. */
public final class WorldMapTileSavedData extends SavedData {
    private static final String ID = "capitalismmod_world_map_tiles";
    private static final int RELIEF_STYLE_VERSION = 3;
    private final Map<String, int[]> tiles = new HashMap<>();
    private final Set<String> shadedDimensions = new HashSet<>();
    private int reliefStyleVersion;

    public record StoredTile(int chunkX, int chunkZ, int[] colors) {}

    private WorldMapTileSavedData() {}

    public static WorldMapTileSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(WorldMapTileSavedData::new, WorldMapTileSavedData::load), ID);
    }

    public int[] get(String key) {
        int[] colors = tiles.get(key);
        return colors == null ? null : colors.clone();
    }

    public void put(String key, int[] colors) {
        if (colors.length != 256) throw new IllegalArgumentException("World map tiles must contain 256 colors");
        tiles.put(key, colors.clone());
        setDirty();
    }

    public void invalidate(String key) {
        if (tiles.remove(key) != null) setDirty();
    }

    public List<StoredTile> getDimensionTiles(String dimension) {
        String prefix = dimension + ":";
        List<StoredTile> result = new ArrayList<>();
        tiles.forEach((key, colors) -> {
            if (!key.startsWith(prefix)) return;
            String[] parts = key.substring(prefix.length()).split(":");
            if (parts.length != 2) return;
            try {
                result.add(new StoredTile(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), colors.clone()));
            } catch (NumberFormatException ignored) {
                // Ignore malformed legacy cache entries.
            }
        });
        return result;
    }

    public boolean isDimensionShaded(String dimension) {
        return reliefStyleVersion >= RELIEF_STYLE_VERSION && shadedDimensions.contains(dimension);
    }

    public void markDimensionShaded(String dimension) {
        shadedDimensions.add(dimension);
        reliefStyleVersion = RELIEF_STYLE_VERSION;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag encoded = new ListTag();
        tiles.forEach((key, colors) -> {
            CompoundTag tile = new CompoundTag();
            tile.putString("key", key);
            tile.put("colors", new IntArrayTag(colors));
            encoded.add(tile);
        });
        tag.put("tiles", encoded);
        tag.putInt("relief_style_version", RELIEF_STYLE_VERSION);
        ListTag shaded = new ListTag();
        shadedDimensions.forEach(dimension -> shaded.add(net.minecraft.nbt.StringTag.valueOf(dimension)));
        tag.put("shaded_dimensions", shaded);
        return tag;
    }

    public static WorldMapTileSavedData load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        WorldMapTileSavedData data = new WorldMapTileSavedData();
        if (tag.contains("tiles", 9)) {
            ListTag encoded = tag.getList("tiles", 10);
            for (int i = 0; i < encoded.size(); i++) {
                CompoundTag tile = encoded.getCompound(i);
                int[] colors = tile.getIntArray("colors");
                if (colors.length == 256 && tile.contains("key")) {
                    data.tiles.put(tile.getString("key"), colors);
                }
            }
        }
        if (tag.contains("shaded_dimensions", 9)) {
            ListTag shaded = tag.getList("shaded_dimensions", 8);
            for (int i = 0; i < shaded.size(); i++) data.shadedDimensions.add(shaded.getString(i));
        }
        data.reliefStyleVersion = tag.getInt("relief_style_version");
        return data;
    }
}
