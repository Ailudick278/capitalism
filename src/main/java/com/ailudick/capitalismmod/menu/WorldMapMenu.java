package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/** Container state for the standalone world map. It intentionally has no land data. */
public class WorldMapMenu extends AbstractContainerMenu {
    public String dimension = "";
    public int chunkX;
    public int chunkZ;
    private final Map<Long, int[]> tiles = new HashMap<>();
    private final Map<Long, com.ailudick.capitalismmod.network.payload.SyncLandOverlayPayload.Cell> landOverlay = new HashMap<>();
    public int selectedChunkX;
    public int selectedChunkZ;
    public boolean hasSelectedChunk;

    public WorldMapMenu(int containerId, Inventory inventory) {
        super(ModMenuTypes.WORLD_MAP_MENU.get(), containerId);
    }

    public void setData(com.ailudick.capitalismmod.network.payload.SyncWorldMapPayload data) {
        dimension = data.dimension();
        chunkX = data.chunkX();
        chunkZ = data.chunkZ();
    }

    public void setTiles(com.ailudick.capitalismmod.network.payload.SyncWorldMapTilesPayload data) {
        for (var tile : data.tiles()) {
            tiles.put(key(tile.chunkX(), tile.chunkZ()), tile.colors());
        }
    }

    public int tileColor(int worldX, int worldZ) {
        int[] colors = tile(Math.floorDiv(worldX, 16), Math.floorDiv(worldZ, 16));
        return colors == null ? 0xFF263746 : colors[Math.floorMod(worldZ, 16) * 16 + Math.floorMod(worldX, 16)];
    }

    public int[] tile(int chunkX, int chunkZ) {
        return tiles.get(key(chunkX, chunkZ));
    }

    public void setLandOverlay(com.ailudick.capitalismmod.network.payload.SyncLandOverlayPayload data) {
        for (var cell : data.cells()) landOverlay.put(key(cell.chunkX(), cell.chunkZ()), cell);
    }

    public boolean isExplored(int chunkX, int chunkZ) {
        return tiles.containsKey(key(chunkX, chunkZ));
    }

    public List<long[]> exploredChunks() {
        return tiles.keySet().stream()
                .map(value -> new long[]{(int) (value >> 32), value.intValue()})
                .toList();
    }

    public com.ailudick.capitalismmod.network.payload.SyncLandOverlayPayload.Cell landCell(int chunkX, int chunkZ) {
        return landOverlay.get(key(chunkX, chunkZ));
    }

    public List<com.ailudick.capitalismmod.network.payload.SyncLandOverlayPayload.Cell> landOverlay() {
        return List.copyOf(landOverlay.values());
    }

    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return true; }

}
