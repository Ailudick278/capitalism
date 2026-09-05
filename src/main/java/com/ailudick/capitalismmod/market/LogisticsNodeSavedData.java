package com.ailudick.capitalismmod.market;

import com.ailudick.capitalismmod.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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

    /**
     * Returns nodes in a loaded dimension and removes entries whose loaded
     * block position no longer contains the registered facility. Unloaded
     * chunks are deliberately left untouched because their block state is not
     * authoritative until the chunk is loaded.
     */
    public List<Node> activeInDimension(ServerLevel level) {
        if (level == null) return List.of();
        String dimension = level.dimension().location().toString();
        boolean changed = false;
        for (Node node : List.copyOf(nodes.values())) {
            if (!dimension.equals(node.dimension()) || !level.hasChunk(node.chunkX(), node.chunkZ())) continue;
            BlockPos pos = new BlockPos(node.x(), node.y(), node.z());
            if (!facilityMatches(level.getBlockState(pos), node.facility())) {
                nodes.remove(node.key());
                changed = true;
            }
        }
        if (changed) setDirty();
        return inDimension(dimension);
    }

    /**
     * Backfills facilities placed before node tracking existed. This is
     * intentionally an explicit, bounded operation rather than a chunk-load
     * hook, so old worlds do not pay a full-height scan on every chunk load.
     */
    public int repair(ServerLevel level, int centerChunkX, int centerChunkZ, int radius) {
        if (level == null) return 0;
        int boundedRadius = Math.max(0, Math.min(8, radius));
        String dimension = level.dimension().location().toString();
        int found = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int chunkZ = centerChunkZ - boundedRadius; chunkZ <= centerChunkZ + boundedRadius; chunkZ++) {
            for (int chunkX = centerChunkX - boundedRadius; chunkX <= centerChunkX + boundedRadius; chunkX++) {
                if (!level.hasChunk(chunkX, chunkZ)) continue;
                int minX = chunkX * 16;
                int minZ = chunkZ * 16;
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        for (int localX = 0; localX < 16; localX++) {
                            mutable.set(minX + localX, y, minZ + localZ);
                            String facility = facilityId(level.getBlockState(mutable));
                            if (facility == null) continue;
                            String nodeKey = key(dimension, mutable.getX(), mutable.getY(), mutable.getZ());
                            if (!nodes.containsKey(nodeKey)) {
                                nodes.put(nodeKey, new Node(nodeKey, dimension, mutable.getX(), mutable.getY(),
                                        mutable.getZ(), facility));
                                found++;
                            }
                        }
                    }
                }
            }
        }
        if (found > 0) setDirty();
        return found;
    }

    private static String facilityId(net.minecraft.world.level.block.state.BlockState state) {
        if (state.is(ModBlocks.LOGISTICS_CENTER_BLOCK.get())) return "logistics_center";
        if (state.is(ModBlocks.TRANSFER_STATION_BLOCK.get())) return "transfer_station";
        if (state.is(ModBlocks.PORT_BLOCK.get())) return "port";
        return null;
    }

    private static boolean facilityMatches(net.minecraft.world.level.block.state.BlockState state, String facility) {
        return facility != null && facility.equals(facilityId(state));
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
