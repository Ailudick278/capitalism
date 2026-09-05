package com.ailudick.capitalismmod.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.menu.LandMenu;
import com.ailudick.capitalismmod.menu.WorldMapMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import com.ailudick.capitalismmod.Config;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.PacketDistributor;
import com.ailudick.capitalismmod.network.payload.RequestWorldMapTilesPayload;
import com.ailudick.capitalismmod.network.payload.RequestLandDetailsPayload;

import java.util.HashMap;
import java.util.Map;

/** Reusable world-map interaction component shared by land and standalone map screens. */
public final class WorldMapWidget {
    private final WorldMapViewport viewport = new WorldMapViewport(0.0, 0.0);
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean centeredOnPlayer;
    private final Map<Long, MapTileTexture> mapTileTextures = new HashMap<>();
    private final Map<Long, TerrainTile> terrainTiles = new HashMap<>();
    private int requestedTileCenterX = Integer.MIN_VALUE;
    private int requestedTileCenterZ = Integer.MIN_VALUE;
    private long lastTileRequestNanos;
    private boolean dragging;
    private double lastMouseX;
    private double lastMouseY;
    private int hoveredChunkX;
    private int hoveredChunkZ;
    private boolean hasHoveredChunk;

    public WorldMapViewport viewport() { return viewport; }

    public void centerOnPlayer() {
        if (!centeredOnPlayer && Minecraft.getInstance().player != null) {
            viewport.centerOn(Minecraft.getInstance().player.getX(), Minecraft.getInstance().player.getZ());
            centeredOnPlayer = true;
        }
    }

    public void drawPlayerMarker(GuiGraphics graphics) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        float blockSize = viewport.zoom();
        float screenX = viewport.screenX(player.getX(), x + width / 2.0F);
        float screenZ = viewport.screenZ(player.getZ(), y + height / 2.0F);
        int centerX = (int) screenX;
        int centerY = (int) screenZ;
        int arm = Math.max(2, Math.round(blockSize * 1.25F));
        int lineWidth = Math.max(1, Math.round(blockSize * 0.15F));
        int marker = 0xFFFF3B30;
        graphics.fill(centerX - arm, centerY - lineWidth / 2, centerX + arm + 1,
                centerY - lineWidth / 2 + lineWidth, marker);
        graphics.fill(centerX - lineWidth / 2, centerY - arm, centerX - lineWidth / 2 + lineWidth,
                centerY + arm + 1, marker);
    }

    public void drawMapTile(GuiGraphics graphics, int chunkX, int chunkZ, int[] colors) {
        float screenX = viewport.screenX(chunkX * 16.0, x + width / 2.0F);
        float screenZ = viewport.screenZ(chunkZ * 16.0, y + height / 2.0F);
        int tileWidth = Math.max(1, (int) Math.ceil(16.0F * viewport.zoom()));
        int tileHeight = Math.max(1, (int) Math.ceil(16.0F * viewport.zoom()));
        if (colors == null) {
            graphics.fill((int) screenX, (int) screenZ, (int) screenX + tileWidth,
                    (int) screenZ + tileHeight, 0xFF263746);
            return;
        }
        long key = ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
        MapTileTexture cached = mapTileTextures.get(key);
        if (cached == null || cached.colors != colors) {
            if (cached != null) {
                Minecraft.getInstance().getTextureManager().release(cached.location);
                cached.texture.close();
            }
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, 16, 16, false);
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    image.setPixelRGBA(localX, localZ, toNativeImageColor(colors[localZ * 16 + localX]));
                }
            }
            DynamicTexture texture = new DynamicTexture(image);
            texture.setFilter(false, false);
            ResourceLocation location = Minecraft.getInstance().getTextureManager()
                    .register("capitalismmod/world_map_tile_" + chunkX + "_" + chunkZ, texture);
            cached = new MapTileTexture(colors, texture, location);
            mapTileTextures.put(key, cached);
        }
        graphics.blit(cached.location, (int) screenX, (int) screenZ, tileWidth, tileHeight,
                0, 0, 16, 16, 16, 16);
    }

    public void drawDiscoveredTerrain(GuiGraphics graphics, WorldMapMenu menu) {
        float blockSize = viewport.zoom();
        double centerWorldX = viewport.centerX();
        double centerWorldZ = viewport.centerZ();
        int minX = (int) Math.floor(centerWorldX - width / (2.0 * blockSize) - 1);
        int maxX = (int) Math.ceil(centerWorldX + width / (2.0 * blockSize) + 1);
        int minZ = (int) Math.floor(centerWorldZ - height / (2.0 * blockSize) - 1);
        int maxZ = (int) Math.ceil(centerWorldZ + height / (2.0 * blockSize) + 1);
        for (int chunkZ = Math.floorDiv(minZ, 16); chunkZ <= Math.floorDiv(maxZ, 16); chunkZ++) {
            for (int chunkX = Math.floorDiv(minX, 16); chunkX <= Math.floorDiv(maxX, 16); chunkX++) {
                drawMapTile(graphics, chunkX, chunkZ, menu.tile(chunkX, chunkZ));
            }
        }
    }

    public void drawDiscoveredTerrain(GuiGraphics graphics, LandMenu menu) {
        float blockSize = viewport.zoom();
        double centerWorldX = viewport.centerX();
        double centerWorldZ = viewport.centerZ();
        int minX = (int) Math.floor(centerWorldX - width / (2.0 * blockSize) - 1);
        int maxX = (int) Math.ceil(centerWorldX + width / (2.0 * blockSize) + 1);
        int minZ = (int) Math.floor(centerWorldZ - height / (2.0 * blockSize) - 1);
        int maxZ = (int) Math.ceil(centerWorldZ + height / (2.0 * blockSize) + 1);
        for (int chunkZ = Math.floorDiv(minZ, 16); chunkZ <= Math.floorDiv(maxZ, 16); chunkZ++) {
            for (int chunkX = Math.floorDiv(minX, 16); chunkX <= Math.floorDiv(maxX, 16); chunkX++) {
                drawMapTile(graphics, chunkX, chunkZ, menu.tile(chunkX, chunkZ));
            }
        }
    }

    public void drawLocalTerrain(GuiGraphics graphics, net.minecraft.client.multiplayer.ClientLevel level) {
        float blockSize = viewport.zoom();
        double centerWorldX = viewport.centerX();
        double centerWorldZ = viewport.centerZ();
        int minX = (int) Math.floor(centerWorldX - width / (2.0 * blockSize) - 1);
        int maxX = (int) Math.ceil(centerWorldX + width / (2.0 * blockSize) + 1);
        int minZ = (int) Math.floor(centerWorldZ - height / (2.0 * blockSize) - 1);
        int maxZ = (int) Math.ceil(centerWorldZ + height / (2.0 * blockSize) + 1);
        for (int chunkZ = Math.floorDiv(minZ, 16); chunkZ <= Math.floorDiv(maxZ, 16); chunkZ++) {
            for (int chunkX = Math.floorDiv(minX, 16); chunkX <= Math.floorDiv(maxX, 16); chunkX++) {
                TerrainTile tile = getTerrainTile(level, chunkX, chunkZ);
                if (tile == null) continue;
                int fromX = Math.max(minX, chunkX * 16);
                int toX = Math.min(maxX, chunkX * 16 + 15);
                int fromZ = Math.max(minZ, chunkZ * 16);
                int toZ = Math.min(maxZ, chunkZ * 16 + 15);
                for (int worldZ = fromZ; worldZ <= toZ; worldZ++) {
                    int runStart = fromX;
                    int runColor = tile.colors[Math.floorMod(worldZ, 16) * 16 + Math.floorMod(runStart, 16)];
                    for (int worldX = fromX + 1; worldX <= toX + 1; worldX++) {
                        int color = worldX <= toX
                                ? tile.colors[Math.floorMod(worldZ, 16) * 16 + Math.floorMod(worldX, 16)]
                                : Integer.MIN_VALUE;
                        if (color != runColor) {
                            drawBlockRun(graphics, runStart, worldX - 1, worldZ, runColor, blockSize);
                            runStart = worldX;
                            runColor = color;
                        }
                    }
                }
            }
        }
    }

    public void drawLandOverlay(GuiGraphics graphics, LandMenu menu) {
        float blockSize = viewport.zoom();
        for (var cell : menu.mapCells) {
            float screenX = viewport.screenX(cell.chunkX() * 16.0, x + width / 2.0F);
            float screenZ = viewport.screenZ(cell.chunkZ() * 16.0, y + height / 2.0F);
            float size = 16.0F * blockSize;
            if (cell.claimed()) {
                graphics.fill((int) screenX, (int) screenZ, (int) Math.ceil(screenX + size),
                        (int) Math.ceil(screenZ + size), cell.auction() ? 0x70D98B36
                                : cell.ownedByPlayer() ? 0x403FA66B : 0x409B4D4D);
            }
            int border = cell.chunkX() == menu.chunkX && cell.chunkZ() == menu.chunkZ
                    ? 0xFFFFFFFF : cell.claimed() ? 0xB0FFFFFF : 0x405A6A78;
            if (menu.hasSelectedChunk && cell.chunkX() == menu.selectedChunkX && cell.chunkZ() == menu.selectedChunkZ) {
                border = 0xFFFFD43B;
            }
            drawBorder(graphics, screenX, screenZ, size, border);
        }
    }

    public void drawWorldMapLandOverlay(GuiGraphics graphics, WorldMapMenu menu,
                                        int hoveredChunkX, int hoveredChunkZ, boolean hasHoveredChunk) {
        float blockSize = viewport.zoom();
        for (long[] explored : menu.exploredChunks()) {
            int chunkX = (int) explored[0];
            int chunkZ = (int) explored[1];
            var cell = menu.landCell(chunkX, chunkZ);
            float screenX = viewport.screenX(chunkX * 16.0, x + width / 2.0F);
            float screenZ = viewport.screenZ(chunkZ * 16.0, y + height / 2.0F);
            float size = 16.0F * blockSize;
            if (cell != null && cell.claimed()) {
                graphics.fill((int) screenX, (int) screenZ, (int) Math.ceil(screenX + size),
                        (int) Math.ceil(screenZ + size), cell.auction() ? 0x70D98B36
                                : cell.ownedByPlayer() ? 0x503FA66B : 0x509B4D4D);
            }
            drawBorder(graphics, screenX, screenZ, size,
                    cell != null && cell.claimed() ? 0xB0FFFFFF : 0x305A6A78);
        }
        if (menu.hasSelectedChunk) {
            float screenX = viewport.screenX(menu.selectedChunkX * 16.0, x + width / 2.0F);
            float screenZ = viewport.screenZ(menu.selectedChunkZ * 16.0, y + height / 2.0F);
            drawBorder(graphics, screenX, screenZ, 16.0F * blockSize, 0xFFFFD43B);
        }
        if (hasHoveredChunk && menu.isExplored(hoveredChunkX, hoveredChunkZ)
                && (!menu.hasSelectedChunk || hoveredChunkX != menu.selectedChunkX || hoveredChunkZ != menu.selectedChunkZ)) {
            float screenX = viewport.screenX(hoveredChunkX * 16.0, x + width / 2.0F);
            float screenZ = viewport.screenZ(hoveredChunkZ * 16.0, y + height / 2.0F);
            drawBorder(graphics, screenX, screenZ, 16.0F * blockSize, 0xFFFFF1A8);
        }
    }

    public void drawChunkGrid(GuiGraphics graphics, LandMenu menu) {
        double centerWorldX = viewport.centerX();
        double centerWorldZ = viewport.centerZ();
        float blockSize = viewport.zoom();
        double minWorldX = centerWorldX - width / (2.0 * blockSize);
        double maxWorldX = centerWorldX + width / (2.0 * blockSize);
        double minWorldZ = centerWorldZ - height / (2.0 * blockSize);
        double maxWorldZ = centerWorldZ + height / (2.0 * blockSize);
        int firstChunkX = ((int) Math.floor(minWorldX / 16.0) - 1) * 16;
        int firstChunkZ = ((int) Math.floor(minWorldZ / 16.0) - 1) * 16;
        int lastChunkX = ((int) Math.ceil(maxWorldX / 16.0) + 1) * 16;
        int lastChunkZ = ((int) Math.ceil(maxWorldZ / 16.0) + 1) * 16;
        for (int worldX = firstChunkX; worldX <= lastChunkX; worldX += 16) {
            float screenX = viewport.screenX(worldX, x + width / 2.0F);
            int chunk = Math.floorDiv(worldX, 16);
            int color = chunk == menu.chunkX ? 0xD0FFFFFF : 0x705A6A78;
            graphics.fill((int) screenX, y, (int) screenX + (chunk == menu.chunkX ? 2 : 1), y + height, color);
        }
        for (int worldZ = firstChunkZ; worldZ <= lastChunkZ; worldZ += 16) {
            float screenZ = viewport.screenZ(worldZ, y + height / 2.0F);
            int chunk = Math.floorDiv(worldZ, 16);
            int color = chunk == menu.chunkZ ? 0xD0FFFFFF : 0x705A6A78;
            graphics.fill(x, (int) screenZ, x + width, (int) screenZ + (chunk == menu.chunkZ ? 2 : 1), color);
        }
    }

    public void drawSelectionInfo(GuiGraphics graphics, Font font, WorldMapMenu menu,
                                  int hoveredChunkX, int hoveredChunkZ, boolean hasHoveredChunk) {
        String text;
        if (!menu.hasSelectedChunk) {
            text = hasHoveredChunk ? "区块 " + hoveredChunkX + ", " + hoveredChunkZ : "未选中区块";
        } else {
            var cell = menu.landCell(menu.selectedChunkX, menu.selectedChunkZ);
            String status = cell == null || !cell.claimed() ? "未占用"
                    : cell.auction() ? "拍卖土地" : cell.ownedByPlayer() ? "我的土地" : "他人土地";
            text = "区块 " + menu.selectedChunkX + ", " + menu.selectedChunkZ + " · " + status;
        }
        int textWidth = font.width(text);
        graphics.fill(x + 4, y + 20, x + textWidth + 14, y + 34, 0xB0101820);
        graphics.drawString(font, Component.literal(text), x + 9, y + 23, GuiStyles.TEXT, true);
    }

    public void requestVisibleTiles(AbstractContainerMenu menu) {
        if (!(menu instanceof WorldMapMenu) && !(menu instanceof LandMenu)) return;
        int centerChunkX = (int) Math.floor(viewport.centerX() / 16.0);
        int centerChunkZ = (int) Math.floor(viewport.centerZ() / 16.0);
        if (centerChunkX == requestedTileCenterX && centerChunkZ == requestedTileCenterZ) return;
        long now = System.nanoTime();
        if (dragging && now - lastTileRequestNanos < 150_000_000L) return;
        requestedTileCenterX = centerChunkX;
        requestedTileCenterZ = centerChunkZ;
        lastTileRequestNanos = now;
        PacketDistributor.sendToServer(new RequestWorldMapTilesPayload(centerChunkX, centerChunkZ,
                Config.WORLD_MAP_DISCOVERY_RADIUS.get(), false));
    }

    public void updateHover(AbstractContainerMenu menu, double mouseX, double mouseY, int left, int top) {
        hasHoveredChunk = false;
        if (!(menu instanceof WorldMapMenu worldMapMenu) || !contains(mouseX, mouseY, left, top)) return;
        int[] chunk = chunkAt(mouseX, mouseY, left, top);
        if (!worldMapMenu.isExplored(chunk[0], chunk[1])) return;
        hoveredChunkX = chunk[0];
        hoveredChunkZ = chunk[1];
        hasHoveredChunk = true;
    }

    public int hoveredChunkX() { return hoveredChunkX; }
    public int hoveredChunkZ() { return hoveredChunkZ; }
    public boolean hasHoveredChunk() { return hasHoveredChunk; }

    public boolean mouseScrolled(AbstractContainerMenu menu, double mouseX, double mouseY,
                                 double scrollY, int left, int top) {
        if (!contains(mouseX, mouseY, left, top)) return false;
        zoomAt(scrollY > 0 ? 1.125 : 1.0 / 1.125, mouseX, mouseY, left, top);
        return true;
    }

    public boolean mouseClicked(AbstractContainerMenu menu, double mouseX, double mouseY,
                                int button, int left, int top) {
        if (!contains(mouseX, mouseY, left, top)) return false;
        if (button == 1 && menu instanceof LandMenu landMenu) {
            landMenu.hasSelectedChunk = false;
            landMenu.selectedChunkX = landMenu.chunkX;
            landMenu.selectedChunkZ = landMenu.chunkZ;
            return true;
        }
        if (button != 0) return false;
        dragging = true;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging || button != 0) return false;
        panPixels(mouseX - lastMouseX, mouseY - lastMouseY);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return true;
    }

    public boolean mouseReleased(AbstractContainerMenu menu, double mouseX, double mouseY,
                                 int button, int left, int top) {
        if (button != 0 || !dragging) return false;
        if (contains(mouseX, mouseY, left, top)
                && Math.abs(mouseX - lastMouseX) < 3.0 && Math.abs(mouseY - lastMouseY) < 3.0) {
            selectChunk(menu, mouseX, mouseY, left, top);
        }
        dragging = false;
        requestVisibleTiles(menu);
        return true;
    }

    private void selectChunk(AbstractContainerMenu menu, double mouseX, double mouseY, int left, int top) {
        int[] chunk = chunkAt(mouseX, mouseY, left, top);
        if (menu instanceof LandMenu landMenu) {
            if (landMenu.mapCells.stream().noneMatch(cell -> cell.chunkX() == chunk[0] && cell.chunkZ() == chunk[1])) return;
            landMenu.selectedChunkX = chunk[0];
            landMenu.selectedChunkZ = chunk[1];
            landMenu.hasSelectedChunk = true;
            PacketDistributor.sendToServer(new RequestLandDetailsPayload(landMenu.dimension, chunk[0], chunk[1]));
        } else if (menu instanceof WorldMapMenu worldMapMenu && worldMapMenu.isExplored(chunk[0], chunk[1])) {
            worldMapMenu.selectedChunkX = chunk[0];
            worldMapMenu.selectedChunkZ = chunk[1];
            worldMapMenu.hasSelectedChunk = true;
        }
    }

    private static void drawBorder(GuiGraphics graphics, float x, float y, float size, int color) {
        graphics.fill((int) x, (int) y, (int) Math.ceil(x + size), (int) y + 1, color);
        graphics.fill((int) x, (int) Math.ceil(y + size) - 1, (int) Math.ceil(x + size), (int) Math.ceil(y + size), color);
        graphics.fill((int) x, (int) y, (int) x + 1, (int) Math.ceil(y + size), color);
        graphics.fill((int) Math.ceil(x + size) - 1, (int) y, (int) Math.ceil(x + size), (int) Math.ceil(y + size), color);
    }

    private TerrainTile getTerrainTile(net.minecraft.client.multiplayer.ClientLevel level, int chunkX, int chunkZ) {
        long key = ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
        TerrainTile cached = terrainTiles.get(key);
        long now = level.getGameTime();
        if (cached != null && now - cached.updatedAt < 10L) return cached;
        if (!level.hasChunkAt(new BlockPos(chunkX * 16, level.getMinBuildHeight(), chunkZ * 16))) return null;
        int[] colors = new int[256];
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int worldX = chunkX * 16 + localX;
                int worldZ = chunkZ * 16 + localZ;
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
                BlockPos pos = new BlockPos(worldX, surfaceY - 1, worldZ);
                BlockState state = level.getBlockState(pos);
                BlockPos canopyPos = findCanopy(level, worldX, worldZ, surfaceY);
                if (canopyPos != null) {
                    pos = canopyPos;
                    state = level.getBlockState(pos);
                }
                colors[localZ * 16 + localX] = mapColor(state, level, pos, surfaceY);
            }
        }
        TerrainTile updated = new TerrainTile(colors, now);
        terrainTiles.put(key, updated);
        return updated;
    }

    private BlockPos findCanopy(net.minecraft.client.multiplayer.ClientLevel level, int worldX, int worldZ, int surfaceY) {
        BlockPos leaves = null;
        BlockPos log = null;
        int top = Math.min(level.getMaxBuildHeight() - 1, surfaceY + 24);
        for (int y = top; y >= surfaceY; y--) {
            BlockState state = level.getBlockState(new BlockPos(worldX, y, worldZ));
            if (state.is(BlockTags.LEAVES)) {
                leaves = new BlockPos(worldX, y, worldZ);
                break;
            }
            if (log == null && state.is(BlockTags.LOGS)) log = new BlockPos(worldX, y, worldZ);
        }
        return leaves != null ? leaves : log;
    }

    private int mapColor(BlockState state, net.minecraft.client.multiplayer.ClientLevel level,
                         BlockPos pos, int surfaceY) {
        if (state.is(BlockTags.LEAVES)) return 0xFF4C8F45;
        if (state.is(BlockTags.LOGS)) return 0xFF8B5A2B;
        MapColor mapColor = state.getMapColor(level, pos);
        int color = mapColor == MapColor.NONE ? 0xFF56616A : 0xFF000000 | mapColor.col;
        int shade = Math.max(0, Math.min(32, (surfaceY - level.getMinBuildHeight()) / 12));
        int red = Math.max(0, ((color >> 16) & 0xFF) - shade);
        int green = Math.max(0, ((color >> 8) & 0xFF) - shade);
        int blue = Math.max(0, (color & 0xFF) - shade);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private void drawBlockRun(GuiGraphics graphics, int fromX, int toX, int worldZ, int color, float blockSize) {
        float screenX = viewport.screenX(fromX + 0.5, x + width / 2.0F);
        float endX = viewport.screenX(toX + 1.5, x + width / 2.0F);
        float screenZ = viewport.screenZ(worldZ + 0.5, y + height / 2.0F);
        graphics.fill((int) screenX, (int) screenZ, Math.max((int) screenX + 1, (int) Math.ceil(endX)),
                Math.max((int) screenZ + 1, (int) Math.ceil(screenZ + blockSize)), color);
    }

    public void close() {
        for (MapTileTexture tile : mapTileTextures.values()) {
            Minecraft.getInstance().getTextureManager().release(tile.location);
            tile.texture.close();
        }
        mapTileTextures.clear();
    }

    private static int toNativeImageColor(int argb) {
        return (argb & 0xFF000000)
                | ((argb & 0x000000FF) << 16)
                | (argb & 0x0000FF00)
                | ((argb & 0x00FF0000) >>> 16);
    }

    private record MapTileTexture(int[] colors, DynamicTexture texture, ResourceLocation location) {}
    private record TerrainTile(int[] colors, long updatedAt) {}

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(double mouseX, double mouseY, int left, int top) {
        double localX = mouseX - left;
        double localY = mouseY - top;
        return localX >= x && localX <= x + width && localY >= y && localY <= y + height;
    }

    public int[] chunkAt(double mouseX, double mouseY, int left, int top) {
        double worldX = viewport.worldXAtScreen(mouseX - left, x + width / 2.0F);
        double worldZ = viewport.worldZAtScreen(mouseY - top, y + height / 2.0F);
        return new int[]{(int) Math.floor(worldX / 16.0), (int) Math.floor(worldZ / 16.0)};
    }

    public void panPixels(double deltaX, double deltaY) { viewport.panPixels(deltaX, deltaY); }

    public void zoomAt(double factor, double mouseX, double mouseY, int left, int top) {
        double anchorX = viewport.worldXAtScreen(mouseX - left, x + width / 2.0F);
        double anchorZ = viewport.worldZAtScreen(mouseY - top, y + height / 2.0F);
        viewport.zoomAt(factor, anchorX, anchorZ);
    }
}
