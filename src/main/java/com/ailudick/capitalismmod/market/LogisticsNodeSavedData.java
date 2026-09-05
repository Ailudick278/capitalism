package com.ailudick.capitalismmod.market;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Persistent positions of logistics facilities for world-map visualization. */
public final class LogisticsNodeSavedData extends SavedData {
    private static final String ID = "capitalismmod_logistics_nodes";
    private final Map<String, Node> nodes = new HashMap<>();

    public record Node(String key, String dimension, int x, int y, int z, String facility) {
        public int chunkX() { return Math.floorDiv(x, 16); }
        public int chunkZ() { return Math.floorDiv(z, 16); }
    }

    private LogisticsNodeSavedData() {}

    public static LogisticsNodeSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LogisticsNodeSavedData::new, LogisticsNodeSavedData::load), ID);
    }

    public void set(String dimension, int x, int y, int z, String facility) {
        if (dimension == null || dimension.isBlank() || facility == null || facility.isBlank()) return;
        String key = key(dimension, x, y, z);
        nodes.put(key, new Node(key, dimension, x, y, z, facility));
        setDirty();
    }

    public void remove(String dimension, int x, int y, int z) {
        if (dimension != null && nodes.remove(key(dimension, x, y, z)) != null) setDirty();
    }

    public List<Node> inDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) return List.of();
        return nodes.values().stream().filter(node -> dimension.equals(node.dimension())).toList();
    }

    private static String key(String dimension, int x, int y, int z) {
        return dimension + ":" + x + ":" + y + ":" + z;
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Node node : nodes.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("key", node.key());
            entry.putString("dimension", node.dimension());
            entry.putInt("x", node.x());
            entry.putInt("y", node.y());
            entry.putInt("z", node.z());
            entry.putString("facility", node.facility());
            list.add(entry);
        }
        tag.put("nodes", list);
        return tag;
    }

    public static LogisticsNodeSavedData load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        LogisticsNodeSavedData data = new LogisticsNodeSavedData();
        ListTag list = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String dimension = entry.getString("dimension");
            String facility = entry.getString("facility");
            if (!dimension.isBlank() && !facility.isBlank()) {
                int x = entry.getInt("x");
                int y = entry.getInt("y");
                int z = entry.getInt("z");
                String key = entry.getString("key");
                if (key.isBlank()) key = key(dimension, x, y, z);
                data.nodes.put(key, new Node(key, dimension, x, y, z, facility));
            }
        }
        return data;
    }
}
