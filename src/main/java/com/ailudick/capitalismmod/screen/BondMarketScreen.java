package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.bond.BondHolding;
import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.menu.BondMarketMenu;
import com.ailudick.capitalismmod.network.payload.BuyBondPayload;
import com.ailudick.capitalismmod.network.payload.RedeemBondPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class BondMarketScreen extends AbstractContainerScreen<BondMarketMenu> {
    private List<BondHolding> builtHoldings = new ArrayList<>();

    public BondMarketScreen(BondMarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 176;
    }

    @Override
    protected void init() {
        super.init();
        refreshWidgets();
    }

    private void refreshWidgets() {
        clearWidgets();
        builtHoldings = new ArrayList<>(menu.getHoldings());

        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.buy_bond"), btn -> buyBond())
                .bounds(leftPos + 8, topPos + 8, 60, 20).build());

        for (int i = 0; i < Math.min(6, menu.getHoldings().size()); i++) {
            final BondHolding holding = menu.getHoldings().get(i);
            addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.redeem"), btn -> redeem(holding.id()))
                    .bounds(leftPos + 160, topPos + 34 + i * 18, 28, 16).build());
        }
    }

    private void buyBond() {
        PacketDistributor.sendToServer(new BuyBondPayload(1));
    }

    private void redeem(String holdingId) {
        PacketDistributor.sendToServer(new RedeemBondPayload(holdingId));
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
        if (!menu.getHoldings().equals(builtHoldings)) {
            refreshWidgets();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        graphics.drawString(font, Component.translatable("gui.capitalismmod.bonds"),
                leftPos + 8, topPos + 32, GuiStyles.TEXT, false);
        for (int i = 0; i < Math.min(6, menu.getHoldings().size()); i++) {
            BondHolding holding = menu.getHoldings().get(i);
            graphics.drawString(font, Component.literal("$" + holding.faceValue() + "  " + holding.daysToMaturity() + "d"),
                    leftPos + 8, topPos + 34 + i * 18, GuiStyles.TEXT, false);
        }
    }
}
