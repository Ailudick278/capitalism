package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.menu.ShopMenu;
import com.ailudick.capitalismmod.network.payload.BuyItemPayload;
import com.ailudick.capitalismmod.shop.ShopOffer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private final List<Button> buyButtons = new ArrayList<>();
    private int lastOfferCount = -1;

    public ShopScreen(ShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 100;
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.getOffers().size() != lastOfferCount) {
            rebuildButtons();
        }
    }

    private void rebuildButtons() {
        for (Button button : buyButtons) {
            removeWidget(button);
        }
        buyButtons.clear();

        List<ShopOffer> offers = menu.getOffers();
        lastOfferCount = offers.size();

        for (int i = 0; i < offers.size(); i++) {
            final int index = i;
            Button button = Button.builder(
                            Component.translatable("gui.capitalismmod.buy"),
                            btn -> PacketDistributor.sendToServer(new BuyItemPayload(index)))
                    .bounds(leftPos + 116, topPos + 8 + i * 22, 40, 18)
                    .build();
            buyButtons.add(button);
            addRenderableWidget(button);
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

        List<ShopOffer> offers = menu.getOffers();
        for (int i = 0; i < offers.size(); i++) {
            ShopOffer offer = offers.get(i);
            int oy = topPos + 8 + i * 22;
            graphics.renderItem(offer.item(), leftPos + 8, oy);
            graphics.renderItemDecorations(font, offer.item(), leftPos + 8, oy);
            graphics.drawString(font, offer.item().getHoverName(), leftPos + 28, oy + 5, GuiStyles.TEXT, false);

            String currencyName = Currencies.exists(offer.currencyId())
                    ? Component.translatable(Currencies.byId(offer.currencyId()).nameKey()).getString()
                    : offer.currencyId();
            graphics.drawString(font, offer.price() + " " + currencyName, leftPos + 88, oy + 5, GuiStyles.TEXT, false);
        }
    }
}
