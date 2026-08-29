package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.Conglomerate;
import com.ailudick.capitalismmod.menu.ConglomerateMenu;
import com.ailudick.capitalismmod.network.payload.RenameConglomeratePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class ConglomerateScreen extends AbstractContainerScreen<ConglomerateMenu> {
    private EditBox nameField;

    public ConglomerateScreen(ConglomerateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 170;
    }

    @Override
    protected void init() {
        super.init();

        this.nameField = new EditBox(font, leftPos + 8, topPos + 30, 120, 20, Component.literal(""));
        this.nameField.setMaxLength(32);
        addRenderableWidget(this.nameField);

        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.rename"), btn -> rename())
                .bounds(leftPos + 132, topPos + 30, 40, 20).build());
    }

    private void rename() {
        String name = this.nameField.getValue();
        if (!name.isBlank()) {
            PacketDistributor.sendToServer(new RenameConglomeratePayload(name));
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

        Conglomerate conglomerate = menu.getConglomerate();
        graphics.drawString(font, Component.translatable("gui.capitalismmod.conglomerate", conglomerate.name()),
                leftPos + 8, topPos + 8, GuiStyles.TEXT, false);

        int y = topPos + 56;
        for (Company company : conglomerate.companies().values()) {
            String line = company.name() + " (Lv." + company.level() + ") - "
                    + Component.translatable("company_type.capitalismmod." + company.type()).getString()
                    + "  $" + company.treasuryOf("usd");
            graphics.drawString(font, line, leftPos + 8, y, GuiStyles.TEXT, false);
            y += 10;
        }
    }
}
