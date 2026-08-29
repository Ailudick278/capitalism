package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;

import com.ailudick.capitalismmod.company.CompanyTypes;
import com.ailudick.capitalismmod.menu.BusinessLicenseMenu;
import com.ailudick.capitalismmod.network.payload.CreateCompanyPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class BusinessLicenseScreen extends AbstractContainerScreen<BusinessLicenseMenu> {
    private static final int COLUMNS = 4;
    private static final int BUTTON_SPACING_X = 42;
    private static final int BUTTON_SPACING_Y = 22;

    private String selectedType = CompanyTypes.ALL.get(0);
    private EditBox nameField;

    public BusinessLicenseScreen(BusinessLicenseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 210;
    }

    @Override
    protected void init() {
        super.init();

        for (int i = 0; i < CompanyTypes.ALL.size(); i++) {
            String type = CompanyTypes.ALL.get(i);
            int row = i / COLUMNS;
            int col = i % COLUMNS;
            addRenderableWidget(Button.builder(Component.translatable(CompanyTypes.shortNameKey(type)), btn -> this.selectedType = type)
                    .bounds(leftPos + 8 + col * BUTTON_SPACING_X, topPos + 34 + row * BUTTON_SPACING_Y, 40, 20).build());
        }

        this.nameField = new EditBox(font, leftPos + 8, topPos + 162, 160, 20, Component.literal(""));
        this.nameField.setMaxLength(32);
        addRenderableWidget(this.nameField);

        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.create"), btn -> create())
                .bounds(leftPos + 8, topPos + 186, 160, 20).build());
    }

    private void create() {
        String name = this.nameField.getValue();
        if (!name.isBlank()) {
            PacketDistributor.sendToServer(new CreateCompanyPayload(this.selectedType, name));
        }
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
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        graphics.drawString(font, title, leftPos + 8, topPos + 6, GuiStyles.ACCENT, false);

        graphics.drawString(font, Component.translatable("gui.capitalismmod.company_type"),
                leftPos + 8, topPos + 20, GuiStyles.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.capitalismmod.company_name"),
                leftPos + 8, topPos + 148, GuiStyles.TEXT, false);
    }
}
