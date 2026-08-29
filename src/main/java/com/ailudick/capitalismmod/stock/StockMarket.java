package com.ailudick.capitalismmod.stock;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.event.EconomyNews;
import com.ailudick.capitalismmod.economy.EconomySavedData;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side stock market: a limit-order book plus periodic price updates.
 *
 * <p>Orders escrow shares (sell) or USD (buy) at placement and auto-match when a new
 * order crosses the book, supporting partial fills. Sellers pay a small stamp duty.
 * Prices mean-revert toward a fundamental value plus supply/demand, clamped to a
 * daily ±10% limit band around the previous close.
 */
public final class StockMarket {
    /** Stamp duty paid by sellers on each fill: 1/1000 = 0.1% (min 1 USD). */
    private static final long STAMP_DUTY_DIVISOR = 1000L;

    private StockMarket() {
    }

    // ---- accessors ----

    public static Map<String, Long> getPrices(MinecraftServer server) {
        return EconomySavedData.get(server).prices();
    }

    public static Map<String, List<Candle>> getHistory(MinecraftServer server) {
        return EconomySavedData.get(server).history();
    }

    public static Map<String, Long> getPortfolio(MinecraftServer server, ServerPlayer player) {
        return EconomySavedData.get(server).portfolio(player.getUUID());
    }

    public static Map<String, String> getCompanyStocks(MinecraftServer server) {
        return EconomySavedData.get(server).listingNames();
    }

    public static List<StockOrder> getOrders(MinecraftServer server) {
        return EconomySavedData.get(server).orders();
    }

    // ---- order placement ----

    /** Places a limit order, matching immediately against crossing orders. */
    public static boolean placeOrder(ServerPlayer player, String stockId, int quantity, long pricePerUnit, boolean sell) {
        EconomySavedData data = EconomySavedData.get(player.getServer());
        if (!data.isStock(stockId) || quantity <= 0 || pricePerUnit <= 0 || !withinLimit(data, stockId, pricePerUnit)) {
            return false;
        }
        boolean success = sell
                ? placeSellOrder(player, data, stockId, quantity, pricePerUnit)
                : placeBuyOrder(player, data, stockId, quantity, pricePerUnit);
        data.setDirty();
        return success;
    }

    /** Cancels the player's own order, returning the escrowed shares or money. */
    public static boolean cancelOrder(ServerPlayer player, String orderId) {
        EconomySavedData data = EconomySavedData.get(player.getServer());
        StockOrder order = data.findOrder(orderId);
        if (order == null || !order.ownerId().equals(player.getStringUUID())) {
            return false;
        }
        data.removeOrder(orderId);
        if (order.sell()) {
            data.addShares(order.stockId(), player.getUUID(), order.quantity());
        } else {
            long total = EconomyMath.multiply(order.quantity(), order.pricePerUnit());
            if (total >= 0) {
                EconomyHelper.giveMoney(player, Currencies.USD, Money.toMinor(total));
            }
        }
        data.setDirty();
        return true;
    }

    // ---- price updates ----

    /** Applies supply/demand and mean reversion (clamped to the daily limit), recording a candle per stock. */
    public static void updatePrices(MinecraftServer server) {
        EconomySavedData data = EconomySavedData.get(server);
        Set<String> ids = new LinkedHashSet<>();
        for (Stock stock : Stocks.ALL) {
            ids.add(stock.id());
        }
        ids.addAll(data.listings().keySet());

        for (String id : ids) {
            long oldPrice = data.price(id);
            long fundamental = data.fundamental(id);
            if (oldPrice <= 0) {
                oldPrice = fundamental;
            }
            long netVolume = data.netVolume(id);
            long newPrice = Math.max(1, oldPrice + (fundamental - oldPrice) / 10 + netVolume / 10);
            newPrice = applyPriceLimit(data, id, newPrice);
            data.putPrice(id, newPrice);
            data.resetNetVolume(id);
            data.addCandle(id, new Candle(oldPrice, Math.max(oldPrice, newPrice), Math.min(oldPrice, newPrice), newPrice));
            if (oldPrice > 0) {
                double percent = (double) (newPrice - oldPrice) / oldPrice * 100.0;
                if (Math.abs(percent) >= 10.0) {
                    EconomyNews.broadcast(server, "news.capitalismmod.price_move",
                            stockDisplayName(data, id), String.format("%+.0f%%", percent));
                }
            }
        }
        data.setDirty();
    }

    /** Rolls the trading day: the previous close becomes the current price for every stock. */
    public static void closeDay(MinecraftServer server) {
        EconomySavedData data = EconomySavedData.get(server);
        Set<String> ids = new LinkedHashSet<>();
        for (Stock stock : Stocks.ALL) {
            ids.add(stock.id());
        }
        ids.addAll(data.listings().keySet());
        for (String id : ids) {
            data.setPrevClose(id, data.price(id));
        }
        data.setDirty();
    }

    private static Component stockDisplayName(EconomySavedData data, String id) {
        Stock stock = Stocks.byId(id);
        if (stock != null) {
            return Component.translatable(stock.nameKey());
        }
        EconomySavedData.Listing listing = data.listings().get(id);
        return listing != null ? Component.literal(listing.name()) : Component.literal(id);
    }

    // ---- matching internals ----

    private static boolean placeBuyOrder(ServerPlayer player, EconomySavedData data, String stockId, int quantity, long pricePerUnit) {
        long total = EconomyMath.multiply(quantity, pricePerUnit);
        if (total < 0 || !EconomyHelper.tryPay(player, Currencies.USD, Money.toMinor(total))) {
            return false;
        }
        int remaining = quantity;
        long spent = 0L;

        List<StockOrder> sells = crossingSells(data, stockId, pricePerUnit);
        for (StockOrder sell : sells) {
            if (remaining <= 0) {
                break;
            }
            int fill = Math.min(remaining, sell.quantity());
            long gross = EconomyMath.multiply(fill, sell.pricePerUnit());
            if (gross < 0) {
                break;
            }
            data.addShares(stockId, player.getUUID(), fill);
            payTo(player.getServer(), UUID.fromString(sell.ownerId()), Money.toMinor(gross - duty(gross)));
            data.addNetVolume(stockId, fill);
            spent += gross;
            remaining -= fill;
            reduceOrRemove(data, sell, fill);
        }

        if (remaining > 0) {
            data.addOrder(new StockOrder(UUID.randomUUID().toString(), player.getStringUUID(),
                    stockId, remaining, pricePerUnit, false));
        }
        long reserved = EconomyMath.multiply(remaining, pricePerUnit);
        long refund = total - spent - reserved;
        if (refund > 0) {
            EconomyHelper.giveMoney(player, Currencies.USD, Money.toMinor(refund));
        }
        return true;
    }

    private static boolean placeSellOrder(ServerPlayer player, EconomySavedData data, String stockId, int quantity, long pricePerUnit) {
        if (data.holdings(stockId, player.getUUID()) < quantity) {
            return false;
        }
        data.addShares(stockId, player.getUUID(), -quantity);
        int remaining = quantity;

        List<StockOrder> buys = crossingBuys(data, stockId, pricePerUnit);
        for (StockOrder buy : buys) {
            if (remaining <= 0) {
                break;
            }
            int fill = Math.min(remaining, buy.quantity());
            long gross = EconomyMath.multiply(fill, buy.pricePerUnit());
            if (gross < 0) {
                break;
            }
            data.addShares(stockId, UUID.fromString(buy.ownerId()), fill);
            EconomyHelper.giveMoney(player, Currencies.USD, Money.toMinor(gross - duty(gross)));
            data.addNetVolume(stockId, -fill);
            remaining -= fill;
            reduceOrRemove(data, buy, fill);
        }

        if (remaining > 0) {
            data.addOrder(new StockOrder(UUID.randomUUID().toString(), player.getStringUUID(),
                    stockId, remaining, pricePerUnit, true));
        }
        return true;
    }

    /** Sell orders for {@code stockId} with ask ≤ {@code limit}, best (lowest) ask first. */
    private static List<StockOrder> crossingSells(EconomySavedData data, String stockId, long limit) {
        List<StockOrder> result = new ArrayList<>();
        for (StockOrder order : data.orders()) {
            if (order.stockId().equals(stockId) && order.sell() && order.pricePerUnit() <= limit) {
                result.add(order);
            }
        }
        result.sort(Comparator.comparingLong(StockOrder::pricePerUnit));
        return result;
    }

    /** Buy orders for {@code stockId} with bid ≥ {@code limit}, best (highest) bid first. */
    private static List<StockOrder> crossingBuys(EconomySavedData data, String stockId, long limit) {
        List<StockOrder> result = new ArrayList<>();
        for (StockOrder order : data.orders()) {
            if (order.stockId().equals(stockId) && !order.sell() && order.pricePerUnit() >= limit) {
                result.add(order);
            }
        }
        result.sort(Comparator.comparingLong(StockOrder::pricePerUnit).reversed());
        return result;
    }

    private static void reduceOrRemove(EconomySavedData data, StockOrder order, int fill) {
        int remaining = order.quantity() - fill;
        if (remaining <= 0) {
            data.removeOrder(order.id());
        } else {
            data.replaceOrder(order.withQuantity(remaining));
        }
    }

    private static long duty(long value) {
        return Math.max(1L, value / STAMP_DUTY_DIVISOR);
    }

    private static boolean withinLimit(EconomySavedData data, String stockId, long price) {
        long prevClose = data.prevClose(stockId);
        if (prevClose <= 0) {
            return true;
        }
        long lower = prevClose * 9 / 10;
        long upper = prevClose * 11 / 10;
        return price >= lower && price <= upper;
    }

    private static long applyPriceLimit(EconomySavedData data, String stockId, long newPrice) {
        long prevClose = data.prevClose(stockId);
        if (prevClose <= 0) {
            return newPrice;
        }
        long lower = prevClose * 9 / 10;
        long upper = prevClose * 11 / 10;
        return Math.max(lower, Math.min(newPrice, upper));
    }

    /** Pays {@code amount} USD to {@code recipientId}, or parks it in the mailbox if offline. */
    private static void payTo(MinecraftServer server, UUID recipientId, long amount) {
        ServerPlayer recipient = server.getPlayerList().getPlayer(recipientId);
        if (recipient != null) {
            EconomyHelper.giveMoney(recipient, Currencies.USD, amount);
        } else {
            MarketMailboxSavedData.get(server).creditMoney(recipientId, "usd", amount);
        }
    }
}
