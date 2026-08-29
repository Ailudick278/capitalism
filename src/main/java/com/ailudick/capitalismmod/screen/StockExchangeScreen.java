package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.menu.StockExchangeMenu;
import com.ailudick.capitalismmod.network.payload.CancelStockOrderPayload;
import com.ailudick.capitalismmod.network.payload.PlaceStockOrderPayload;
import com.ailudick.capitalismmod.stock.Candle;
import com.ailudick.capitalismmod.stock.Stock;
import com.ailudick.capitalismmod.stock.StockOrder;
import com.ailudick.capitalismmod.stock.Stocks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockExchangeScreen extends AbstractContainerScreen<StockExchangeMenu> {
    private static final int COLOR_UP = 0xFFFF5555;   // up (red)
    private static final int COLOR_DOWN = 0xFF55FF55; // down (green)
    private static final int BOOK_DEPTH = 3;

    private String selectedId = Stocks.ALL.isEmpty() ? "" : Stocks.ALL.get(0).id();
    private EditBox quantityField;
    private EditBox priceField;
    private Map<String, String> builtCompanies = new HashMap<>();
    private List<StockOrder> builtOrders = new ArrayList<>();

    public StockExchangeScreen(StockExchangeMenu menu, Inventory inventory, Component title) {
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
        builtCompanies = new HashMap<>(menu.getCompanies());
        builtOrders = new ArrayList<>(menu.getOrders());

        int i = 0;
        for (Stock stock : Stocks.ALL) {
            addRenderableWidget(Button.builder(Component.translatable(stock.nameKey()), btn -> this.selectedId = stock.id())
                    .bounds(leftPos + 8, topPos + 8 + i * 20, 80, 18).build());
            i++;
        }
        for (Map.Entry<String, String> entry : builtCompanies.entrySet()) {
            addRenderableWidget(Button.builder(Component.literal(entry.getValue()), btn -> this.selectedId = entry.getKey())
                    .bounds(leftPos + 8, topPos + 8 + i * 20, 80, 18).build());
            i++;
        }

        this.quantityField = new EditBox(font, leftPos + 8, topPos + 210, 40, 20, Component.literal("1"));
        this.quantityField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.quantityField.setValue(qty.isEmpty() ? "1" : qty);
        addRenderableWidget(this.quantityField);

        this.priceField = new EditBox(font, leftPos + 52, topPos + 210, 50, 20, Component.literal(""));
        this.priceField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.priceField.setValue(price);
        addRenderableWidget(this.priceField);

        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.buy"), btn -> placeOrder(false))
                .bounds(leftPos + 106, topPos + 210, 34, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.sell"), btn -> placeOrder(true))
                .bounds(leftPos + 143, topPos + 210, 34, 20).build());

        rebuildCancelButtons();
    }

    private void rebuildCancelButtons() {
        int sellRow = 0;
        for (StockOrder order : sellOrders()) {
            if (sellRow >= BOOK_DEPTH) {
                break;
            }
            if (isOwn(order)) {
                addRenderableWidget(Button.builder(Component.literal("X"), btn -> cancel(order.id()))
                        .bounds(leftPos + 146, topPos + 78 + sellRow * 18, 16, 16).build());
            }
            sellRow++;
        }
        int buyRow = 0;
        for (StockOrder order : buyOrders()) {
            if (buyRow >= BOOK_DEPTH) {
                break;
            }
            if (isOwn(order)) {
                addRenderableWidget(Button.builder(Component.literal("X"), btn -> cancel(order.id()))
                        .bounds(leftPos + 146, topPos + 138 + buyRow * 18, 16, 16).build());
            }
            buyRow++;
        }
    }

    private void placeOrder(boolean sell) {
        try {
            int quantity = Integer.parseInt(this.quantityField.getValue());
            long price = Long.parseLong(this.priceField.getValue());
            if (quantity > 0 && price > 0) {
                PacketDistributor.sendToServer(new PlaceStockOrderPayload(selectedId, quantity, price, sell));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void cancel(String orderId) {
        PacketDistributor.sendToServer(new CancelStockOrderPayload(orderId));
    }

    private boolean isOwn(StockOrder order) {
        return minecraft != null && minecraft.player != null
                && order.ownerId().equals(minecraft.player.getStringUUID());
    }

    private List<StockOrder> sellOrders() {
        List<StockOrder> result = new ArrayList<>();
        for (StockOrder order : menu.getOrders()) {
            if (order.stockId().equals(selectedId) && order.sell()) {
                result.add(order);
            }
        }
        result.sort(Comparator.comparingLong(StockOrder::pricePerUnit));
        return result;
    }

    private List<StockOrder> buyOrders() {
        List<StockOrder> result = new ArrayList<>();
        for (StockOrder order : menu.getOrders()) {
            if (order.stockId().equals(selectedId) && !order.sell()) {
                result.add(order);
            }
        }
        result.sort(Comparator.comparingLong(StockOrder::pricePerUnit).reversed());
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
        if (!menu.getCompanies().equals(builtCompanies) || !menu.getOrders().equals(builtOrders)) {
            refreshWidgets();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        graphics.drawString(font, title, leftPos + 8, topPos + 6, GuiStyles.ACCENT, false);

        long price = menu.getPrices().getOrDefault(selectedId, 0L);
        long holdings = menu.getPortfolio().getOrDefault(selectedId, 0L);
        List<Candle> candles = menu.getHistory().getOrDefault(selectedId, List.of());

        graphics.drawString(font, Component.translatable("gui.capitalismmod.stock_price", price),
                leftPos + 94, topPos + 8, GuiStyles.TEXT, false);
        if (!candles.isEmpty()) {
            double percent = candles.get(candles.size() - 1).percentChange();
            int color = percent >= 0 ? COLOR_UP : COLOR_DOWN;
            graphics.drawString(font, Component.literal(String.format("%+.2f%%", percent)),
                    leftPos + 94, topPos + 18, color, false);
        }
        graphics.drawString(font, Component.translatable("gui.capitalismmod.holdings", holdings),
                leftPos + 94, topPos + 28, GuiStyles.TEXT, false);

        List<StockOrder> buys = buyOrders();
        List<StockOrder> sells = sellOrders();
        if (!buys.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.capitalismmod.best_bid", buys.get(0).pricePerUnit()),
                    leftPos + 94, topPos + 40, GuiStyles.TEXT, false);
        }
        if (!sells.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.capitalismmod.best_ask", sells.get(0).pricePerUnit()),
                    leftPos + 94, topPos + 50, GuiStyles.TEXT, false);
        }

        graphics.drawString(font, Component.translatable("gui.capitalismmod.sell_orders"),
                leftPos + 8, topPos + 68, GuiStyles.TEXT, false);
        for (int i = 0; i < Math.min(BOOK_DEPTH, sells.size()); i++) {
            StockOrder order = sells.get(i);
            graphics.drawString(font, order.quantity() + " @ $" + order.pricePerUnit(),
                    leftPos + 8, topPos + 78 + i * 18, GuiStyles.TEXT, false);
        }

        graphics.drawString(font, Component.translatable("gui.capitalismmod.buy_orders"),
                leftPos + 8, topPos + 128, GuiStyles.TEXT, false);
        for (int i = 0; i < Math.min(BOOK_DEPTH, buys.size()); i++) {
            StockOrder order = buys.get(i);
            graphics.drawString(font, order.quantity() + " @ $" + order.pricePerUnit(),
                    leftPos + 8, topPos + 138 + i * 18, GuiStyles.TEXT, false);
        }
    }
}
