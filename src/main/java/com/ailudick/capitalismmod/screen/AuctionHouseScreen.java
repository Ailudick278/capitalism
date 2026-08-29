package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.auction.Auction;
import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.menu.AuctionHouseMenu;
import com.ailudick.capitalismmod.network.payload.BidPayload;
import com.ailudick.capitalismmod.network.payload.ListAuctionPayload;
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

public class AuctionHouseScreen extends AbstractContainerScreen<AuctionHouseMenu> {
    private int selectedCommodity = 0;
    private EditBox quantityField;
    private EditBox priceField;
    private List<Auction> builtAuctions = new ArrayList<>();

    public AuctionHouseScreen(AuctionHouseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 230;
    }

    @Override
    protected void init() {
        super.init();
        refreshWidgets();
    }

    private void refreshWidgets() {
        String qty = this.quantityField != null ? this.quantityField.getValue() : "1";
        String price = this.priceField != null ? this.priceField.getValue() : "10";
        clearWidgets();
        builtAuctions = new ArrayList<>(menu.getAuctions());

        for (int i = 0; i < Commodities.ALL.size(); i++) {
            final int index = i;
            int row = i / 3;
            int col = i % 3;
            addRenderableWidget(Button.builder(Commodities.get(i).getHoverName(), btn -> this.selectedCommodity = index)
                    .bounds(leftPos + 8 + col * 55, topPos + 4 + row * 22, 50, 20).build());
        }

        this.quantityField = new EditBox(font, leftPos + 8, topPos + 50, 40, 20, Component.literal("1"));
        this.quantityField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.quantityField.setValue(qty);
        addRenderableWidget(this.quantityField);

        this.priceField = new EditBox(font, leftPos + 52, topPos + 50, 50, 20, Component.literal("10"));
        this.priceField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.priceField.setValue(price);
        addRenderableWidget(this.priceField);

        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.list_auction"), btn -> listAuction())
                .bounds(leftPos + 106, topPos + 50, 40, 20).build());

        for (int i = 0; i < Math.min(6, menu.getAuctions().size()); i++) {
            final Auction auction = menu.getAuctions().get(i);
            addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.bid"), btn -> bid(auction))
                    .bounds(leftPos + 160, topPos + 76 + i * 18, 28, 16).build());
        }
    }

    private void listAuction() {
        try {
            int qty = Integer.parseInt(this.quantityField.getValue());
            long start = Long.parseLong(this.priceField.getValue());
            if (qty > 0 && start > 0) {
                PacketDistributor.sendToServer(new ListAuctionPayload(selectedCommodity, qty, start, 300));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void bid(Auction auction) {
        long bid = auction.currentBidder().isEmpty()
                ? auction.startingPrice()
                : auction.currentBid() + Math.max(1, auction.currentBid() / 10);
        PacketDistributor.sendToServer(new BidPayload(auction.id(), bid));
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
        if (!menu.getAuctions().equals(builtAuctions)) {
            refreshWidgets();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        graphics.drawString(font, Component.translatable("gui.capitalismmod.auctions"),
                leftPos + 8, topPos + 74, GuiStyles.TEXT, false);
        for (int i = 0; i < Math.min(6, menu.getAuctions().size()); i++) {
            Auction auction = menu.getAuctions().get(i);
            ItemStack item = Commodities.byId(auction.itemId());
            String name = item != null ? item.getHoverName().getString() : auction.itemId();
            long price = auction.currentBidder().isEmpty() ? auction.startingPrice() : auction.currentBid();
            graphics.drawString(font, Component.literal(auction.quantity() + "x " + name + " $" + price),
                    leftPos + 8, topPos + 76 + i * 18, GuiStyles.TEXT, false);
        }
    }
}
