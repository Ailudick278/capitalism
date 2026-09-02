package com.ailudick.capitalismmod.market;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.event.EconomyNews;
import com.ailudick.capitalismmod.event.TradeCompletedEvent;
import com.ailudick.capitalismmod.stock.Candle;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side commodity exchange: a limit order book plus periodic price updates.
 *
 * <p>Sell orders escrow the commodity from the exchange's delivery warehouse
 * (see {@link WarehouseSavedData}); buy orders escrow USD. A new order auto-matches
 * against the book with partial fills, price-time priority. Sellers pay a small
 * commission. Prices mean-revert toward a fundamental (config) value plus
 * supply/demand (trade volume plus company production/consumption), clamped to a
 * daily price-limit band around the previous close.
 */
public final class CommodityMarket {
    /** Commission paid by sellers on each fill: 1/1000 = 0.1% (min 1 USD). */
    private static final long COMMISSION_DIVISOR = 1000L;

    private CommodityMarket() {
    }

    // ---- accessors ----

    public static List<MarketOrder> getOrders(MinecraftServer server) {
        return CommoditySavedData.get(server).orders();
    }

    public static java.util.Map<String, Long> getPrices(MinecraftServer server) {
        return CommoditySavedData.get(server).prices();
    }

    public static java.util.Map<String, List<Candle>> getHistory(MinecraftServer server) {
        return CommoditySavedData.get(server).history();
    }

    // ---- order placement ----

    /** Places a sell order, matching immediately against crossing buy orders. */
    public static boolean placeSell(ServerPlayer player, int commodityIndex, int quantity, long pricePerUnit) {
        if (!Commodities.isValid(commodityIndex) || quantity <= 0 || pricePerUnit <= 0) {
            return false;
        }
        ItemStack commodity = Commodities.get(commodityIndex).copy();
        String itemId = Commodities.id(commodity);
        CommoditySavedData data = CommoditySavedData.get(player.getServer());
        if (!withinLimit(data, itemId, pricePerUnit)) {
            return false;
        }
        WarehouseSavedData warehouse = WarehouseSavedData.get(player.getServer());
        if (!warehouse.consume(player.getUUID(), commodity.getItem(), quantity)) {
            return false;
        }

        int remaining = quantity;
        for (MarketOrder buy : crossingBuys(data, itemId, pricePerUnit)) {
            if (remaining <= 0) {
                break;
            }
            int fill = Math.min(remaining, buy.quantity());
            long gross = EconomyMath.multiply(fill, buy.pricePerUnit());
            if (gross < 0) {
                break;
            }
            UUID buyerId = UUID.fromString(buy.ownerId());
            warehouse.credit(buyerId, commodity.getItem(), fill);
            EconomyHelper.giveMoney(player, Currencies.USD, Money.toMinor(gross - commission(gross)));
            data.addNetVolume(itemId, -fill);
            remaining -= fill;
            reduceOrRemove(data, buy, fill);
            NeoForge.EVENT_BUS.post(new TradeCompletedEvent(null, player, commodity, fill, "usd", gross,
                    "commodity", commission(gross)));
        }

        if (remaining > 0) {
            data.addOrder(new MarketOrder(UUID.randomUUID().toString(), player.getStringUUID(),
                    commodity.copy(), remaining, pricePerUnit, true));
        }
        data.setDirty();
        return true;
    }

    /** Places a buy order, matching immediately against crossing sell orders. */
    public static boolean placeBuy(ServerPlayer player, int commodityIndex, int quantity, long pricePerUnit) {
        if (!Commodities.isValid(commodityIndex) || quantity <= 0 || pricePerUnit <= 0) {
            return false;
        }
        ItemStack commodity = Commodities.get(commodityIndex).copy();
        String itemId = Commodities.id(commodity);
        CommoditySavedData data = CommoditySavedData.get(player.getServer());
        if (!withinLimit(data, itemId, pricePerUnit)) {
            return false;
        }
        long total = EconomyMath.multiply(quantity, pricePerUnit);
        if (total < 0 || !EconomyHelper.tryPay(player, Currencies.USD, Money.toMinor(total))) {
            return false;
        }
        WarehouseSavedData warehouse = WarehouseSavedData.get(player.getServer());

        int remaining = quantity;
        long spent = 0L;
        for (MarketOrder sell : crossingSells(data, itemId, pricePerUnit)) {
            if (remaining <= 0) {
                break;
            }
            int fill = Math.min(remaining, sell.quantity());
            long gross = EconomyMath.multiply(fill, sell.pricePerUnit());
            if (gross < 0) {
                break;
            }
            warehouse.credit(player.getUUID(), commodity.getItem(), fill);
            UUID sellerId = UUID.fromString(sell.ownerId());
            ServerPlayer seller = player.getServer().getPlayerList().getPlayer(sellerId);
            payOrPend(player.getServer(), seller, sellerId, gross - commission(gross));
            data.addNetVolume(itemId, fill);
            spent += gross;
            remaining -= fill;
            reduceOrRemove(data, sell, fill);
            NeoForge.EVENT_BUS.post(new TradeCompletedEvent(player, seller, commodity, fill, "usd", gross,
                    "commodity", commission(gross)));
        }

        if (remaining > 0) {
            data.addOrder(new MarketOrder(UUID.randomUUID().toString(), player.getStringUUID(),
                    commodity.copy(), remaining, pricePerUnit, false));
        }
        long reserved = EconomyMath.multiply(remaining, pricePerUnit);
        long refund = total - spent - reserved;
        if (refund > 0) {
            EconomyHelper.giveMoney(player, Currencies.USD, Money.toMinor(refund));
        }
        data.setDirty();
        return true;
    }

    /** Cancels the player's own order, returning the escrowed commodity or money. */
    public static boolean cancelOrder(ServerPlayer player, String orderId) {
        CommoditySavedData data = CommoditySavedData.get(player.getServer());
        MarketOrder order = data.findOrder(orderId);
        if (order == null || !order.ownerId().equals(player.getStringUUID())) {
            return false;
        }
        data.removeOrder(orderId);
        if (order.sell()) {
            WarehouseSavedData.get(player.getServer()).credit(player.getUUID(), order.commodity().getItem(), order.quantity());
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

    /** Applies mean reversion and supply/demand to each commodity, recording a candle. */
    public static void updatePrices(MinecraftServer server) {
        CommoditySavedData data = CommoditySavedData.get(server);
        Set<String> ids = new LinkedHashSet<>();
        for (ItemStack stack : Commodities.ALL) {
            ids.add(Commodities.id(stack));
        }
        for (String id : ids) {
            long oldPrice = data.price(id);
            long fundamental = data.fundamental(id);
            if (oldPrice <= 0) {
                oldPrice = fundamental;
            }
            long netVolume = data.netVolume(id);
            long supply = data.supply(id);
            long newPrice = Math.max(1, oldPrice + (fundamental - oldPrice) / 10 + (netVolume + supply) / 10);
            newPrice = applyPriceLimit(data, id, newPrice);
            data.putPrice(id, newPrice);
            data.resetNetVolume(id);
            data.resetSupply(id);
            data.addCandle(id, new Candle(oldPrice, Math.max(oldPrice, newPrice), Math.min(oldPrice, newPrice), newPrice));
            broadcastPriceMove(server, id, oldPrice, newPrice);
        }
        data.setDirty();
    }

    private static void broadcastPriceMove(MinecraftServer server, String itemId, long oldPrice, long newPrice) {
        if (oldPrice <= 0) {
            return;
        }
        double percent = (double) (newPrice - oldPrice) / oldPrice * 100.0;
        if (Math.abs(percent) >= 10.0) {
            ItemStack commodity = Commodities.byId(itemId);
            Component name = commodity != null ? commodity.getHoverName() : Component.literal(itemId);
            EconomyNews.broadcast(server, "news.capitalismmod.price_move", name, String.format("%+.0f%%", percent));
        }
    }

    /** Rolls the trading day: the previous close becomes the current price for every commodity. */
    public static void closeDay(MinecraftServer server) {
        CommoditySavedData data = CommoditySavedData.get(server);
        for (ItemStack stack : Commodities.ALL) {
            String id = Commodities.id(stack);
            data.setPrevClose(id, data.price(id));
        }
        data.setDirty();
    }

    // ---- matching internals ----

    /** Buy orders for {@code itemId} with bid ≥ {@code limit}, best (highest) bid first. */
    private static List<MarketOrder> crossingBuys(CommoditySavedData data, String itemId, long limit) {
        List<MarketOrder> result = new ArrayList<>();
        for (MarketOrder order : data.orders()) {
            if (!order.sell() && Commodities.id(order.commodity()).equals(itemId) && order.pricePerUnit() >= limit) {
                result.add(order);
            }
        }
        result.sort(Comparator.comparingLong(MarketOrder::pricePerUnit).reversed());
        return result;
    }

    /** Sell orders for {@code itemId} with ask ≤ {@code limit}, best (lowest) ask first. */
    private static List<MarketOrder> crossingSells(CommoditySavedData data, String itemId, long limit) {
        List<MarketOrder> result = new ArrayList<>();
        for (MarketOrder order : data.orders()) {
            if (order.sell() && Commodities.id(order.commodity()).equals(itemId) && order.pricePerUnit() <= limit) {
                result.add(order);
            }
        }
        result.sort(Comparator.comparingLong(MarketOrder::pricePerUnit));
        return result;
    }

    private static void reduceOrRemove(CommoditySavedData data, MarketOrder order, int fill) {
        int remaining = order.quantity() - fill;
        if (remaining <= 0) {
            data.removeOrder(order.id());
        } else {
            data.replaceOrder(order.withQuantity(remaining));
        }
    }

    private static long commission(long value) {
        return Math.max(1L, value / COMMISSION_DIVISOR);
    }

    private static boolean withinLimit(CommoditySavedData data, String itemId, long price) {
        long prevClose = data.prevClose(itemId);
        if (prevClose <= 0) {
            return true;
        }
        double limit = Config.COMMODITY_PRICE_LIMIT.get();
        long lower = (long) (prevClose * (1 - limit));
        long upper = (long) (prevClose * (1 + limit));
        return price >= lower && price <= upper;
    }

    private static long applyPriceLimit(CommoditySavedData data, String itemId, long newPrice) {
        long prevClose = data.prevClose(itemId);
        if (prevClose <= 0) {
            return newPrice;
        }
        double limit = Config.COMMODITY_PRICE_LIMIT.get();
        long lower = (long) (prevClose * (1 - limit));
        long upper = (long) (prevClose * (1 + limit));
        return Math.max(lower, Math.min(newPrice, upper));
    }

    /** Pays {@code amount} USD to {@code recipient}, or parks it in the mailbox if they are offline. */
    private static void payOrPend(MinecraftServer server, ServerPlayer recipient, UUID recipientId, long amount) {
        if (recipient != null) {
            EconomyHelper.giveMoney(recipient, Currencies.USD, Money.toMinor(amount));
        } else {
            MarketMailboxSavedData.get(server).creditMoney(recipientId, Currencies.USD.id(), Money.toMinor(amount));
        }
    }
}
