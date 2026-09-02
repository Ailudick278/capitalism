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

/** Trading-terminal layout inspired by modern stock quote pages. */
public class StockExchangeScreen extends AbstractContainerScreen<StockExchangeMenu> {
    private enum View { MARKET, QUOTE, TRADING, PORTFOLIO }

    private static final int WIDTH = 500;
    private static final int HEIGHT = 350;
    private static final int CONTENT_X = 30;
    private static final int CHART_X = CONTENT_X;
    private static final int CHART_Y = 112;
    private static final int CHART_W = 462;
    private static final int CHART_H = 137;
    private static final int UP = 0xFFE84C4C;
    private static final int DOWN = 0xFF17A673;
    private static final int GRID = 0xFF30343F;
    private static final int PANEL = 0xD9161720;

    private String selectedId = Stocks.ALL.isEmpty() ? "" : Stocks.ALL.get(0).id();
    private View view = View.MARKET;
    private int page;
    private int period;
    private boolean candleMode;
    private double chartMouseX = -1;
    private double chartMouseY = -1;
    private EditBox quantityField;
    private EditBox priceField;
    private Map<String, String> builtCompanies = new HashMap<>();
    private List<StockOrder> builtOrders = new ArrayList<>();

    public StockExchangeScreen(StockExchangeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        refreshWidgets();
    }

    private void refreshWidgets() {
        String quantity = quantityField == null ? "1" : quantityField.getValue();
        String price = priceField == null ? "" : priceField.getValue();
        clearWidgets();
        builtCompanies = new HashMap<>(menu.getCompanies());
        builtOrders = new ArrayList<>(menu.getOrders());
        selectFirstVisible();

        if (view == View.TRADING) {
            quantityField = new EditBox(font, leftPos + 74, topPos + 315, 58, 20, Component.literal("数量"));
            quantityField.setFilter(v -> v.isEmpty() || v.matches("\\d+"));
            quantityField.setValue(quantity.isEmpty() ? "1" : quantity);
            addRenderableWidget(quantityField);
            priceField = new EditBox(font, leftPos + 140, topPos + 315, 88, 20, Component.literal("限价"));
            priceField.setFilter(v -> v.isEmpty() || v.matches("\\d+"));
            priceField.setValue(price);
            addRenderableWidget(priceField);
            addRenderableWidget(Button.builder(Component.literal("买入"), b -> placeOrder(false))
                    .bounds(leftPos + 238, topPos + 315, 58, 20).build());
            addRenderableWidget(Button.builder(Component.literal("卖出"), b -> placeOrder(true))
                    .bounds(leftPos + 302, topPos + 315, 58, 20).build());
            addCancelButtons();
        }
    }

    private void setView(View next) {
        view = next;
        refreshWidgets();
    }

    private void setPeriod(int selectedPeriod) {
        period = selectedPeriod;
        candleMode = selectedPeriod > 0;
    }

    private void changePage(int delta) {
        page = Math.max(0, Math.min(page + delta, pageCount() - 1));
        refreshWidgets();
    }

    private void selectFirstVisible() {
        List<String> ids = visibleIds();
        if (!ids.contains(selectedId)) selectedId = ids.isEmpty() ? "" : ids.get(0);
    }

    private void drawTextNavigation(GuiGraphics graphics) {
        String[] pages = {"行情", "个股", "交易", "我的证券"};
        for (int i = 0; i < pages.length; i++) {
            int px = leftPos + CONTENT_X + i * 58;
            boolean selected = (view == View.MARKET && i == 0) || (view == View.QUOTE && i == 1)
                    || (view == View.TRADING && i == 2) || (view == View.PORTFOLIO && i == 3);
            graphics.drawString(font, Component.literal(pages[i]), px, topPos + 60,
                    selected ? GuiStyles.ACCENT : GuiStyles.TEXT_DIM, false);
            if (selected) graphics.fill(px, topPos + 71, px + font.width(pages[i]), topPos + 73, GuiStyles.ACCENT);
        }
        if (view == View.MARKET) drawMarketList(graphics);
        if (view == View.QUOTE) drawPeriodTabs(graphics);
    }

    private void drawMarketList(GuiGraphics graphics) {
        graphics.drawString(font, Component.literal("上市公司 / 最新行情"), leftPos + CONTENT_X, topPos + 84, GuiStyles.TEXT_DIM, false);
        List<String> ids = visibleIds();
        for (int i = 0; i < ids.size(); i++) {
            int px = leftPos + CONTENT_X + (i % 3) * 150;
            int py = topPos + 104 + (i / 3) * 18;
            String id = ids.get(i);
            long price = menu.getPrices().getOrDefault(id, 0L);
            graphics.drawString(font, Component.literal(fit(shortName(id), 76)), px, py,
                    id.equals(selectedId) ? GuiStyles.TEXT : GuiStyles.TEXT_DIM, false);
            graphics.drawString(font, Component.literal(price > 0 ? "$" + price : "--"), px + 82, py,
                    GuiStyles.TEXT, false);
        }
        graphics.drawString(font, Component.literal("‹"), leftPos + 452, topPos + 104,
                page > 0 ? GuiStyles.TEXT : GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("›"), leftPos + 474, topPos + 104,
                page + 1 < pageCount() ? GuiStyles.TEXT : GuiStyles.TEXT_DIM, false);
    }

    private void drawPeriodTabs(GuiGraphics graphics) {
        String[] periods = {"分时", "日K", "周K", "月K", "季K", "年K"};
        for (int i = 0; i < periods.length; i++) {
            int px = leftPos + CONTENT_X + i * 42;
            int color = i == period ? GuiStyles.ACCENT : GuiStyles.TEXT_DIM;
            graphics.drawString(font, Component.literal(periods[i]), px, topPos + 84, color, false);
            if (i == period) graphics.fill(px, topPos + 95, px + font.width(periods[i]), topPos + 97, GuiStyles.ACCENT);
        }
    }

    private List<String> allIds() {
        List<String> ids = new ArrayList<>();
        for (Stock stock : Stocks.ALL) ids.add(stock.id());
        ids.addAll(builtCompanies.keySet());
        return ids;
    }

    private List<String> visibleIds() {
        List<String> ids = allIds();
        int start = Math.min(page * 12, ids.size());
        return new ArrayList<>(ids.subList(start, Math.min(start + 12, ids.size())));
    }

    private int pageCount() {
        return Math.max(1, (allIds().size() + 11) / 12);
    }

    private String shortName(String id) {
        Stock stock = Stocks.byId(id);
        return stock == null ? builtCompanies.getOrDefault(id, id) : Component.translatable(stock.nameKey()).getString();
    }

    private String name() {
        if (selectedId.isBlank()) return "暂无已上市公司";
        return shortName(selectedId);
    }

    private void addCancelButtons() {
        int row = 0;
        for (StockOrder order : sells()) {
            if (row >= 2) break;
            if (isOwn(order)) addCancel(order.id(), 158, 282 + row * 14);
            row++;
        }
        row = 0;
        for (StockOrder order : buys()) {
            if (row >= 2) break;
            if (isOwn(order)) addCancel(order.id(), 338, 282 + row * 14);
            row++;
        }
    }

    private void addCancel(String id, int x, int y) {
        addRenderableWidget(Button.builder(Component.literal("×"), b -> cancel(id))
                .bounds(leftPos + x, topPos + y, 18, 15).build());
    }

    private void placeOrder(boolean sell) {
        try {
            int quantity = Integer.parseInt(quantityField.getValue());
            long price = Long.parseLong(priceField.getValue());
            if (quantity > 0 && price > 0 && !selectedId.isBlank())
                PacketDistributor.sendToServer(new PlaceStockOrderPayload(selectedId, quantity, price, sell));
        } catch (NumberFormatException ignored) { }
    }

    private void cancel(String id) { PacketDistributor.sendToServer(new CancelStockOrderPayload(id)); }

    private boolean isOwn(StockOrder order) {
        return minecraft != null && minecraft.player != null && order.ownerId().equals(minecraft.player.getStringUUID());
    }

    private List<StockOrder> sells() {
        List<StockOrder> result = new ArrayList<>();
        for (StockOrder order : menu.getOrders()) if (order.stockId().equals(selectedId) && order.sell()) result.add(order);
        result.sort(Comparator.comparingLong(StockOrder::pricePerUnit));
        return result;
    }

    private List<StockOrder> buys() {
        List<StockOrder> result = new ArrayList<>();
        for (StockOrder order : menu.getOrders()) if (order.stockId().equals(selectedId) && !order.sell()) result.add(order);
        result.sort(Comparator.comparingLong(StockOrder::pricePerUnit).reversed());
        return result;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        chartMouseX = mouseX;
        chartMouseY = mouseY;
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        if (y >= 54 && y <= 76) {
            for (int i = 0; i < 4; i++) {
                if (x >= CONTENT_X + i * 58 && x < CONTENT_X + i * 58 + 52) {
                    setView(View.values()[i]);
                    return true;
                }
            }
        }
        if (view == View.QUOTE && y >= 78 && y <= 100) {
            for (int i = 0; i < 6; i++) {
                if (x >= CONTENT_X + i * 42 && x < CONTENT_X + i * 42 + 38) {
                    setPeriod(i);
                    return true;
                }
            }
        }
        if (view == View.MARKET && y >= 98 && y < 170) {
            List<String> ids = visibleIds();
            for (int i = 0; i < ids.size(); i++) {
                int itemX = CONTENT_X + (i % 3) * 150;
                int itemY = 100 + (i / 3) * 18;
                if (x >= itemX && x < itemX + 135 && y >= itemY && y < itemY + 16) {
                    selectedId = ids.get(i);
                    setView(View.QUOTE);
                    return true;
                }
            }
            if (x >= 448 && x < 472) { changePage(-1); return true; }
            if (x >= 472 && x < 500) { changePage(1); return true; }
        }
        if (view == View.QUOTE && y >= 112 && y <= 250 && x >= 390 && x < 492) {
            setView(View.TRADING);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) { }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiStyles.drawBackground(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + CONTENT_X, topPos + 4, leftPos + 492, topPos + 50, PANEL);
        graphics.fill(leftPos + CONTENT_X, topPos + 74, leftPos + 492, topPos + 108, PANEL);
        graphics.fill(leftPos + CONTENT_X, topPos + 110, leftPos + 492, topPos + 252, 0xD9101118);
        graphics.fill(leftPos + CONTENT_X, topPos + 264, leftPos + 492, topPos + 344, PANEL);
        graphics.fill(leftPos + 8, topPos + 4, leftPos + CONTENT_X - 3, topPos + 344, 0xEE20212A);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!menu.getCompanies().equals(builtCompanies) || !menu.getOrders().equals(builtOrders)) refreshWidgets();
        super.render(graphics, mouseX, mouseY, partialTick);
        // super.render() draws the background, so the chart must be rendered afterwards.
        if (view == View.QUOTE) drawChart(graphics);
        drawTextNavigation(graphics);

        long price = menu.getPrices().getOrDefault(selectedId, 0L);
        long holdings = menu.getPortfolio().getOrDefault(selectedId, 0L);
        List<Candle> candles = menu.getHistory().getOrDefault(selectedId, List.of());
        double change = candles.isEmpty() ? 0 : candles.get(candles.size() - 1).percentChange();

        graphics.drawString(font, title, leftPos + 38, topPos + 10, GuiStyles.ACCENT, false);
        String displayName = fit(name(), 88);
        graphics.drawString(font, Component.literal(displayName), leftPos + 38, topPos + 28, GuiStyles.TEXT, false);
        int codeX = leftPos + 42 + font.width(displayName);
        graphics.drawString(font, Component.literal(selectedId.isBlank() ? "" : selectedId), codeX, topPos + 28,
                GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("$" + price), leftPos + 176, topPos + 20, GuiStyles.TEXT, false);
        graphics.drawString(font, Component.literal(String.format("%+.2f%%", change)), leftPos + 250, topPos + 22,
                change >= 0 ? UP : DOWN, false);
        graphics.drawString(font, Component.literal("持仓 " + holdings + " 股"), leftPos + 350, topPos + 20, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("高  " + high(candles) + "     低  " + low(candles) + "     开  " + open(candles)),
                leftPos + 176, topPos + 38, GuiStyles.TEXT_DIM, false);
        if (view == View.QUOTE) {
            graphics.drawString(font, Component.literal((candleMode ? "K线" : "分时") + " · 成交量"), leftPos + CONTENT_X, topPos + 106, GuiStyles.TEXT_DIM, false);
            graphics.drawString(font, Component.literal("点击此处进入交易"), leftPos + 388, topPos + 106, GuiStyles.ACCENT, false);
        }

        if (view == View.TRADING) drawTradingPanel(graphics, price);
        if (view == View.PORTFOLIO) drawPortfolio(graphics);
        drawToolbar(graphics);
        drawCrosshair(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void drawTradingPanel(GuiGraphics graphics, long price) {
        List<StockOrder> sells = sells();
        List<StockOrder> buys = buys();
        graphics.drawString(font, Component.literal("卖盘 Ask"), leftPos + 38, topPos + 270, GuiStyles.ACCENT, false);
        graphics.drawString(font, Component.literal("买盘 Bid"), leftPos + 218, topPos + 270, GuiStyles.ACCENT, false);
        for (int i = 0; i < 3; i++) {
            String ask = i < sells.size() ? sells.get(i).quantity() + "  $" + sells.get(i).pricePerUnit()
                    : (20 + i * 10) + "  $" + (price + 2L + i * 2L);
            String bid = i < buys.size() ? buys.get(i).quantity() + "  $" + buys.get(i).pricePerUnit()
                    : (20 + i * 10) + "  $" + Math.max(1L, price - 2L - i * 2L);
            graphics.drawString(font, Component.literal(ask), leftPos + 38, topPos + 284 + i * 12,
                    i < sells.size() ? GuiStyles.TEXT : UP, false);
            graphics.drawString(font, Component.literal(bid), leftPos + 218, topPos + 284 + i * 12,
                    i < buys.size() ? GuiStyles.TEXT : DOWN, false);
        }
        graphics.drawString(font, Component.literal("委托"), leftPos + 38, topPos + 321, GuiStyles.TEXT_DIM, false);
    }

    private void drawPortfolio(GuiGraphics graphics) {
        graphics.drawString(font, Component.literal("我的证券"), leftPos + CONTENT_X, topPos + 84, GuiStyles.ACCENT, false);
        graphics.drawString(font, Component.literal("股票"), leftPos + CONTENT_X, topPos + 105, GuiStyles.TEXT_DIM, false);
        graphics.drawString(font, Component.literal("持仓"), leftPos + 260, topPos + 105, GuiStyles.TEXT_DIM, false);
        int row = 0;
        for (String id : allIds()) {
            long held = menu.getPortfolio().getOrDefault(id, 0L);
            long price = menu.getPrices().getOrDefault(id, 0L);
            graphics.drawString(font, Component.literal(fit(shortName(id), 160)), leftPos + CONTENT_X, topPos + 124 + row * 18, GuiStyles.TEXT, false);
            graphics.drawString(font, Component.literal(held + " 股   $" + price), leftPos + 260, topPos + 124 + row * 18, GuiStyles.TEXT, false);
            if (++row >= 10) break;
        }
        if (row == 0) graphics.drawString(font, Component.literal("暂无持仓"), leftPos + CONTENT_X, topPos + 124, GuiStyles.TEXT_DIM, false);
    }

    private long high(List<Candle> c) { return c.isEmpty() ? 0 : c.get(c.size() - 1).high(); }
    private long low(List<Candle> c) { return c.isEmpty() ? 0 : c.get(c.size() - 1).low(); }
    private long open(List<Candle> c) { return c.isEmpty() ? 0 : c.get(c.size() - 1).open(); }

    private String fit(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        String suffix = "…";
        while (value.length() > 1 && font.width(value + suffix) > maxWidth)
            value = value.substring(0, value.length() - 1);
        return value + suffix;
    }

    /** Small vector toolbar so the exchange does not depend on missing texture assets. */
    private void drawToolbar(GuiGraphics g) {
        int x = leftPos + 18;
        int color = 0xFFD7D8DE;
        for (int i = 0; i < 8; i++) {
            int y = topPos + 18 + i * 39;
            g.fill(x - 8, y - 8, x + 8, y + 8, i == 0 ? 0xFF3C3E48 : 0x552A2B34);
            switch (i) {
                case 0 -> { g.hLine(x - 6, x + 6, y, color); g.vLine(x, y - 6, y + 6, color); }
                case 1 -> { g.fill(x - 6, y + 4, x - 3, y + 7, color); g.fill(x - 1, y, x + 2, y + 7, color); g.fill(x + 4, y - 6, x + 7, y + 7, color); }
                case 2 -> { g.hLine(x - 7, x + 7, y + 5, color); g.hLine(x - 7, x + 7, y - 5, color); g.vLine(x - 6, y - 5, y + 5, color); g.vLine(x + 6, y - 5, y + 5, color); }
                case 3 -> { g.vLine(x - 5, y - 7, y + 7, color); g.vLine(x, y - 3, y + 7, color); g.vLine(x + 5, y - 7, y + 3, color); }
                case 4 -> { g.fill(x - 7, y - 6, x + 7, y - 4, color); g.fill(x - 7, y - 1, x + 7, y + 1, color); g.fill(x - 7, y + 4, x + 7, y + 6, color); }
                case 5 -> { g.hLine(x - 7, x + 7, y, color); g.vLine(x, y - 7, y + 7, color); g.fill(x - 2, y - 2, x + 3, y + 3, 0xFF3C3E48); }
                case 6 -> { g.fill(x - 7, y - 5, x + 7, y - 3, color); g.fill(x - 4, y - 1, x + 4, y + 1, color); g.fill(x - 7, y + 3, x + 7, y + 5, color); }
                case 7 -> { g.hLine(x - 7, x + 7, y + 6, color); g.vLine(x - 6, y - 6, y + 6, color); g.vLine(x + 6, y - 6, y + 6, color); }
            }
        }
    }

    private void drawChart(GuiGraphics graphics) {
        int x = leftPos + CHART_X, y = topPos + CHART_Y;
        for (int i = 1; i < 7; i++) graphics.hLine(x, x + CHART_W, y + i * 24, GRID);
        for (int i = 1; i < 10; i++) graphics.vLine(x + i * 60, y, y + CHART_H, GRID);
        List<Candle> candles = menu.getHistory().getOrDefault(selectedId, List.of());
        if (candles.isEmpty()) {
            graphics.drawString(font, Component.literal("暂无行情历史"), x + 210, y + 58, GuiStyles.TEXT_DIM, false);
            return;
        }
        int count = Math.min(period == 0 ? 90 : 60, candles.size());
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (int i = candles.size() - count; i < candles.size(); i++) { min = Math.min(min, candles.get(i).low()); max = Math.max(max, candles.get(i).high()); }
        if (min == max) max = min + 1;
        if (candleMode) drawCandles(graphics, candles, count, min, max, x, y);
        else drawIntradayLine(graphics, candles, count, min, max, x, y);
        graphics.drawString(font, Component.literal("VOL"), x + 4, y + CHART_H - 14, GuiStyles.TEXT_DIM, false);
    }

    private void drawIntradayLine(GuiGraphics graphics, List<Candle> candles, int count, long min, long max, int x, int y) {
        for (int i = 1; i < count; i++) {
            Candle before = candles.get(candles.size() - count + i - 1);
            Candle current = candles.get(candles.size() - count + i);
            int x1 = x + (i - 1) * CHART_W / Math.max(1, count - 1);
            int x2 = x + i * CHART_W / Math.max(1, count - 1);
            int y1 = chartY(before.close(), min, max, y);
            int y2 = chartY(current.close(), min, max, y);
            line(graphics, x1, y1, x2, y2, current.close() >= before.close() ? UP : DOWN);
        }
        drawVolume(graphics, candles, count, x, y);
    }

    private void drawCandles(GuiGraphics graphics, List<Candle> candles, int count, long min, long max, int x, int y) {
        int candleWidth = Math.max(3, CHART_W / Math.max(1, count) - 2);
        for (int i = 0; i < count; i++) {
            Candle c = candles.get(candles.size() - count + i);
            int cx = x + i * CHART_W / Math.max(1, count - 1);
            int high = chartY(c.high(), min, max, y);
            int low = chartY(c.low(), min, max, y);
            int open = chartY(c.open(), min, max, y);
            int close = chartY(c.close(), min, max, y);
            int color = c.close() >= c.open() ? UP : DOWN;
            graphics.vLine(cx, high, low + 1, color);
            graphics.fill(cx - candleWidth / 2, Math.min(open, close), cx + candleWidth / 2 + 1,
                    Math.max(open, close) + 1, color);
        }
        drawVolume(graphics, candles, count, x, y);
    }

    private void drawVolume(GuiGraphics graphics, List<Candle> candles, int count, int x, int y) {
        int base = y + CHART_H - 5;
        for (int i = 0; i < count; i++) {
            Candle c = candles.get(candles.size() - count + i);
            int height = Math.max(2, (int) Math.min(22, Math.abs(c.close() - c.open()) + 2));
            int cx = x + i * CHART_W / Math.max(1, count - 1);
            graphics.fill(cx, base - height, cx + 2, base, c.close() >= c.open() ? UP : DOWN);
        }
    }

    private void drawCrosshair(GuiGraphics graphics) {
        int x = leftPos + CHART_X, y = topPos + CHART_Y;
        if (chartMouseX < x || chartMouseX > x + CHART_W || chartMouseY < y || chartMouseY > y + CHART_H) return;
        int crossX = (int) chartMouseX;
        int crossY = (int) chartMouseY;
        graphics.vLine(crossX, y, y + CHART_H, 0xFF8A8F9B);
        graphics.hLine(x, x + CHART_W, crossY, 0xFF8A8F9B);
        List<Candle> candles = menu.getHistory().getOrDefault(selectedId, List.of());
        if (candles.isEmpty()) return;
        int count = Math.min(period == 0 ? 90 : 60, candles.size());
        int index = Math.max(0, Math.min(count - 1, (int) ((chartMouseX - x) * count / CHART_W)));
        Candle c = candles.get(candles.size() - count + index);
        graphics.fill(crossX + 4, crossY - 10, crossX + 94, crossY + 4, 0xE820222B);
        graphics.drawString(font, Component.literal("O " + c.open() + " H " + c.high() + " L " + c.low() + " C " + c.close()),
                crossX + 7, crossY - 8, GuiStyles.TEXT, false);
    }

    private int chartY(long price, long min, long max, int y) {
        return y + CHART_H - 20 - (int) ((price - min) * (CHART_H - 28) / Math.max(1L, max - min));
    }

    private void line(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / Math.max(1, steps);
            int y = y1 + (y2 - y1) * i / Math.max(1, steps);
            g.fill(x, y, x + 2, y + 2, color);
        }
    }
}
