package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.menu.CompanyMenu;
import com.ailudick.capitalismmod.network.payload.UpgradeCompanyPayload;
import com.ailudick.capitalismmod.network.payload.WithdrawCompanyPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

public class CompanyScreen extends AbstractContainerScreen<CompanyMenu> {
    private long lastSignature = -1;

    public CompanyScreen(CompanyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 220;
    }

    @Override
    protected void init() {
        super.init();
        refreshWidgets();
    }

    private void refreshWidgets() {
        clearWidgets();
        int i = 0;
        for (Map.Entry<String, Company> entry : menu.getCompanies().entrySet()) {
            String name = entry.getKey();
            int y = topPos + 24 + i * 26;
            addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.upgrade"), btn -> upgrade(name))
                    .bounds(leftPos + 102, y, 34, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.withdraw"), btn -> withdraw(name))
                    .bounds(leftPos + 138, y, 34, 16).build());
            i++;
        }
        lastSignature = signature();
    }

    private long signature() {
        long s = 0;
        for (Company company : menu.getCompanies().values()) {
            s += company.level() + company.treasuryOf("usd") + company.taxOwed();
        }
        return s;
    }

    private void upgrade(String name) {
        PacketDistributor.sendToServer(new UpgradeCompanyPayload(name));
    }

    private void withdraw(String name) {
        PacketDistributor.sendToServer(new WithdrawCompanyPayload(name));
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
        if (signature() != lastSignature) {
            refreshWidgets();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        graphics.drawString(font, title, leftPos + 8, topPos + 6, GuiStyles.ACCENT, false);

        int i = 0;
        for (Map.Entry<String, Company> entry : menu.getCompanies().entrySet()) {
            String name = entry.getKey();
            Company company = entry.getValue();
            int y = topPos + 28 + i * 26;
            graphics.drawString(font, name + " Lv." + company.level(), leftPos + 8, y, GuiStyles.TEXT, false);
            graphics.drawString(font, "$" + company.treasuryOf("usd") + " 税$" + company.taxOwed(),
                    leftPos + 8, y + 11, GuiStyles.TEXT_DIM, false);
            i++;
        }
    }
}
