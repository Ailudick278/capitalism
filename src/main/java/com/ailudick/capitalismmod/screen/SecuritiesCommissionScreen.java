package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.menu.SecuritiesCommissionMenu;
import com.ailudick.capitalismmod.network.payload.IpoCompanyPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SecuritiesCommissionScreen extends AbstractContainerScreen<SecuritiesCommissionMenu> {
    private Set<String> builtListed = new HashSet<>();

    public SecuritiesCommissionScreen(SecuritiesCommissionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 190;
    }

    @Override
    protected void init() {
        super.init();
        refreshWidgets();
    }

    /** Rebuilds the "IPO" buttons for every unlisted company. */
    private void refreshWidgets() {
        clearWidgets();
        int i = 0;
        for (Map.Entry<String, Company> entry : menu.getCompanies().entrySet()) {
            String name = entry.getKey();
            if (!menu.isListed(name)) {
                int y = topPos + 28 + i * 24;
                addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.ipo"), btn -> ipo(name))
                        .bounds(leftPos + 128, y, 40, 20).build());
            }
            i++;
        }
        builtListed = new HashSet<>(menu.getListed());
    }

    private void ipo(String name) {
        PacketDistributor.sendToServer(new IpoCompanyPayload(name));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiStyles.drawBackground(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!menu.getListed().equals(builtListed)) {
            refreshWidgets();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        graphics.drawString(font, title, leftPos + 8, topPos + 6, GuiStyles.ACCENT, false);

        int i = 0;
        for (Map.Entry<String, Company> entry : menu.getCompanies().entrySet()) {
            String name = entry.getKey();
            Company company = entry.getValue();
            int y = topPos + 34 + i * 24;
            graphics.drawString(font, name + " (Lv." + company.level() + ")", leftPos + 8, y, GuiStyles.TEXT, false);
            if (menu.isListed(name)) {
                graphics.drawString(font, Component.translatable("gui.capitalismmod.listed"), leftPos + 128, y, GuiStyles.TEXT_DIM, false);
            }
            i++;
        }
    }
}
