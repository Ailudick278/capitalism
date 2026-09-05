package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.menu.WarehouseMenu;
import com.ailudick.capitalismmod.network.payload.WarehouseDepositPayload;
import com.ailudick.capitalismmod.network.payload.WarehouseWithdrawPayload;
import com.ailudick.capitalismmod.network.payload.SelectWarehouseOwnerPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class WarehouseScreen extends AbstractContainerScreen<WarehouseMenu> {
    private EditBox quantityField;
    private final List<Button> ownerButtons = new ArrayList<>();

    public WarehouseScreen(WarehouseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 176;
    }

    @Override
    protected void init() {
        super.init();
        this.quantityField = new EditBox(font, leftPos + 8, topPos + 6, 40, 20, Component.literal("1"));
        this.quantityField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.quantityField.setValue("1");
        addRenderableWidget(this.quantityField);

        rebuildOwnerButtons();

        for (int i = 0; i < Commodities.ALL.size(); i++) {
            final int index = i;
            int y = topPos + 32 + i * 22;
            addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.deposit"), btn -> deposit(index))
                    .bounds(leftPos + 126, y, 28, 18).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.withdraw"), btn -> withdraw(index))
                    .bounds(leftPos + 156, y, 28, 18).build());
        }
    }

    public void rebuildOwnerButtons() {
        for (Button button : ownerButtons) removeWidget(button);
        ownerButtons.clear();
        int index = 0;
        for (var entry : menu.getOwners().entrySet()) {
            final String key = entry.getKey();
            Button button = Button.builder(Component.literal(entry.getValue()), btn ->
                    PacketDistributor.sendToServer(new SelectWarehouseOwnerPayload(key)))
                    .bounds(leftPos + 52 + index * 72, topPos + 6, 68, 20).build();
            ownerButtons.add(addRenderableWidget(button));
            if (++index >= 2) break;
        }
    }

    private void deposit(int index) {
        try {
            int count = Integer.parseInt(this.quantityField.getValue());
            if (count > 0) {
                PacketDistributor.sendToServer(new WarehouseDepositPayload(menu.getOwnerKey(), index, count));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void withdraw(int index) {
        try {
            int count = Integer.parseInt(this.quantityField.getValue());
            if (count > 0) {
                PacketDistributor.sendToServer(new WarehouseWithdrawPayload(menu.getOwnerKey(), index, count));
            }
        } catch (NumberFormatException ignored) {
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

        for (int i = 0; i < Commodities.ALL.size(); i++) {
            ItemStack commodity = Commodities.get(i);
            int y = topPos + 32 + i * 22;
            graphics.renderItem(commodity, leftPos + 8, y);
            graphics.renderItemDecorations(font, commodity, leftPos + 8, y);
            graphics.drawString(font, commodity.getHoverName(), leftPos + 28, y + 5, GuiStyles.TEXT, false);
            int count = menu.getStorage().getOrDefault(Commodities.id(commodity), 0);
            graphics.drawString(font, Component.literal(String.valueOf(count)), leftPos + 96, y + 5, GuiStyles.TEXT, false);
        }
    }
}
