package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.market.MarketOrder;
import com.ailudick.capitalismmod.menu.CommodityExchangeMenu;
import com.ailudick.capitalismmod.network.payload.CancelOrderPayload;
import com.ailudick.capitalismmod.network.payload.PlaceOrderPayload;
import com.ailudick.capitalismmod.stock.Candle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CommodityExchangeScreen extends AbstractContainerScreen<CommodityExchangeMenu> {
    private static final int COLOR_UP = 0xFFFF5555;   // up (red)
    private static final int COLOR_DOWN = 0xFF55FF55; // down (green)
    private static final int BOOK_DEPTH = 3;

    private int selectedCommodity = 0;
    private EditBox quantityField;
    private EditBox priceField;
    private List<MarketOrder> builtOrders = new ArrayList<>();

    public CommodityExchangeScreen(CommodityExchangeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 235;
    }

    @Override
    protected void init() {
        super.init();
        refreshWidgets();
    }

    private void refreshWidgets() {
        String qty = this.quantityField != null ? this.quantityField.getValue() : "1";
        String price = this.priceField != null ? this.priceField.getValue() : "";
        clearWidgets();
        builtOrders = new ArrayList<>(menu.getOrders());

        // commodity selection (two rows of three)
        for (int i = 0; i < Commodities.ALL.size(); i++) {
            final int index = i;
            int row = i / 3;
            int col = i % 3;
            addRenderableWidget(Button.builder(Commodities.get(i).getHoverName(), btn -> this.selectedCommodity = index)
                    .bounds(leftPos + 8 + col * 55, topPos + 4 + row * 22, 50, 20).build());
        }

        this.quantityField = new EditBox(font, leftPos + 8, topPos + 214, 40, 20, Component.literal("1"));
        this.quantityField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.quantityField.setValue(qty.isEmpty() ? "1" : qty);
        addRenderableWidget(this.quantityField);

        this.priceField = new EditBox(font, leftPos + 52, topPos + 214, 50, 20, Component.literal(""));
        this.priceField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.priceField.setValue(price);
        addRenderableWidget(this.priceField);

        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.buy"), btn -> placeOrder(false))
                .bounds(leftPos + 106, topPos + 214, 34, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.sell"), btn -> placeOrder(true))
                .bounds(leftPos + 143, topPos + 214, 34, 20).build());

        rebuildCancelButtons();
    }

    private void rebuildCancelButtons() {
        int sellRow = 0;
        for (MarketOrder order : sellOrders()) {
            if (sellRow >= BOOK_DEPTH) {
                break;
            }
            if (isOwn(order)) {
                addRenderableWidget(Button.builder(Component.literal("X"), btn -> cancel(order.id()))
                        .bounds(leftPos + 146, topPos + 94 + sellRow * 18, 16, 16).build());
            }
            sellRow++;
        }
        int buyRow = 0;
        for (MarketOrder order : buyOrders()) {
            if (buyRow >= BOOK_DEPTH) {
                break;
            }
            if (isOwn(order)) {
                addRenderableWidget(Button.builder(Component.literal("X"), btn -> cancel(order.id()))
                        .bounds(leftPos + 146, topPos + 160 + buyRow * 18, 16, 16).build());
            }
            buyRow++;
        }
    }

    private void placeOrder(boolean sell) {
        try {
            int quantity = Integer.parseInt(this.quantityField.getValue());
            long price = Long.parseLong(this.priceField.getValue());
            if (quantity > 0 && price > 0) {
                PacketDistributor.sendToServer(new PlaceOrderPayload(selectedCommodity, quantity, price, sell));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void cancel(String orderId) {
        PacketDistributor.sendToServer(new CancelOrderPayload(orderId));
    }

    private boolean isOwn(MarketOrder order) {
        return minecraft != null && minecraft.player != null
                && order.ownerId().equals(minecraft.player.getStringUUID());
    }

    private String selectedId() {
        return Commodities.id(Commodities.get(selectedCommodity));
    }

    private List<MarketOrder> sellOrders() {
        List<MarketOrder> result = new ArrayList<>();
        String id = selectedId();
        for (MarketOrder order : menu.getOrders()) {
            if (order.sell() && Commodities.id(order.commodity()).equals(id)) {
                result.add(order);
            }
        }
        result.sort(Comparator.comparingLong(MarketOrder::pricePerUnit));
        return result;
    }

    private List<MarketOrder> buyOrders() {
        List<MarketOrder> result = new ArrayList<>();
        String id = selectedId();
        for (MarketOrder order : menu.getOrders()) {
            if (!order.sell() && Commodities.id(order.commodity()).equals(id)) {
                result.add(order);
            }
        }
        result.sort(Comparator.comparingLong(MarketOrder::pricePerUnit).reversed());
        return result;
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
        if (!menu.getOrders().equals(builtOrders)) {
            refreshWidgets();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        String id = selectedId();
        long price = menu.getPrices().getOrDefault(id, 0L);
        List<Candle> candles = menu.getHistory().getOrDefault(id, List.of());

        graphics.drawString(font, Component.translatable("gui.capitalismmod.stock_price", price),
                leftPos + 8, topPos + 50, GuiStyles.TEXT, false);
        if (!candles.isEmpty()) {
            double percent = candles.get(candles.size() - 1).percentChange();
            int color = percent >= 0 ? COLOR_UP : COLOR_DOWN;
            graphics.drawString(font, Component.literal(String.format("%+.2f%%", percent)),
                    leftPos + 100, topPos + 50, color, false);
        }

        List<MarketOrder> buys = buyOrders();
        List<MarketOrder> sells = sellOrders();
        if (!buys.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.capitalismmod.best_bid", buys.get(0).pricePerUnit()),
                    leftPos + 8, topPos + 64, GuiStyles.TEXT, false);
        }
        if (!sells.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.capitalismmod.best_ask", sells.get(0).pricePerUnit()),
                    leftPos + 100, topPos + 64, GuiStyles.TEXT, false);
        }

        graphics.drawString(font, Component.translatable("gui.capitalismmod.sell_orders"),
                leftPos + 8, topPos + 82, GuiStyles.TEXT, false);
        for (int i = 0; i < Math.min(BOOK_DEPTH, sells.size()); i++) {
            MarketOrder order = sells.get(i);
            graphics.drawString(font, order.quantity() + " @ $" + order.pricePerUnit(),
                    leftPos + 8, topPos + 94 + i * 18, GuiStyles.TEXT, false);
        }

        graphics.drawString(font, Component.translatable("gui.capitalismmod.buy_orders"),
                leftPos + 8, topPos + 148, GuiStyles.TEXT, false);
        for (int i = 0; i < Math.min(BOOK_DEPTH, buys.size()); i++) {
            MarketOrder order = buys.get(i);
            graphics.drawString(font, order.quantity() + " @ $" + order.pricePerUnit(),
                    leftPos + 8, topPos + 160 + i * 18, GuiStyles.TEXT, false);
        }
    }
}
