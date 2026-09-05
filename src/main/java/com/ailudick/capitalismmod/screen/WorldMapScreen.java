package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.menu.WorldMapMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Standalone world map screen. It shares only the map widget, not the land screen. */
public final class WorldMapScreen extends AbstractContainerScreen<WorldMapMenu> {
    private final WorldMapWidget worldMapWidget = new WorldMapWidget();
    private final WorldMapViewport viewport = worldMapWidget.viewport();
    private int mapX;
    private int mapY;
    private int mapWidth;
    private int mapHeight;

    public WorldMapScreen(WorldMapMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        WorldMapClientState.load(viewport);
        worldMapWidget.centerOnPlayer();
        imageWidth = 300;
        imageHeight = 190;
    }

    @Override
    protected void init() {
        imageWidth = width;
        imageHeight = height;
        super.init();
        leftPos = 0;
        topPos = 0;
        mapX = 0;
        mapY = 0;
        mapWidth = width;
        mapHeight = height;
        worldMapWidget.setBounds(mapX, mapY, mapWidth, mapHeight);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0xFF101820);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        worldMapWidget.updateHover(menu, mouseX, mouseY, leftPos, topPos);
        worldMapWidget.requestVisibleTiles(menu);
        graphics.fill(mapX - 2, mapY - 2, mapX + mapWidth + 2,
                mapY + mapHeight + 2, 0xCC101820);
        graphics.enableScissor(leftPos + mapX, topPos + mapY,
                leftPos + mapX + mapWidth, topPos + mapY + mapHeight);
        var level = Minecraft.getInstance().level;
        if (level == null) {
            graphics.fill(mapX, mapY, mapX + mapWidth, mapY + mapHeight, 0xFF263746);
        } else {
            worldMapWidget.drawDiscoveredTerrain(graphics, menu);
        }
        worldMapWidget.drawWorldMapLandOverlay(graphics, menu,
                worldMapWidget.hoveredChunkX(), worldMapWidget.hoveredChunkZ(),
                worldMapWidget.hasHoveredChunk());
        worldMapWidget.drawPlayerMarker(graphics);
        graphics.disableScissor();
        graphics.drawString(font,
                Component.literal(String.format(java.util.Locale.ROOT, "缩放：×%.4f", viewport.zoom())),
                mapX + 6, mapY + 6, GuiStyles.TEXT, true);
        worldMapWidget.drawSelectionInfo(graphics, font, menu,
                worldMapWidget.hoveredChunkX(), worldMapWidget.hoveredChunkZ(),
                worldMapWidget.hasHoveredChunk());
    }

    @Override
    public void removed() {
        WorldMapClientState.save(viewport);
        worldMapWidget.close();
        super.removed();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (worldMapWidget.mouseScrolled(menu, mouseX, mouseY, scrollY, leftPos, topPos)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (worldMapWidget.mouseClicked(menu, mouseX, mouseY, button, leftPos, topPos)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (worldMapWidget.mouseReleased(menu, mouseX, mouseY, button, leftPos, topPos)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (worldMapWidget.mouseDragged(mouseX, mouseY, button)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}
