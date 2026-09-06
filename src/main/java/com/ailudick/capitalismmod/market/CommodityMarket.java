package com.ailudick.capitalismmod.market;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.event.TradeCompletedEvent;
import com.ailudick.capitalismmod.stock.Candle;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.ailudick.capitalismmod.tax.TaxTransactionService;
import com.ailudick.capitalismmod.tax.TaxType;
import com.ailudick.capitalismmod.population.PopulationSavedData;
import com.ailudick.capitalismmod.economy.FinancialSettlementJournalSavedData;
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
        String orderId = UUID.randomUUID().toString();
        CommoditySellIntentSavedData intents = CommoditySellIntentSavedData.get(player.getServer());
        intents.add(new CommoditySellIntentSavedData.Intent(orderId, player.getUUID(), itemId, quantity,
                pricePerUnit, player.getServer().overworld().getGameTime(),
                warehouse.count(player.getUUID(), itemId), false));
        if (!warehouse.consume(player.getUUID(), commodity.getItem(), quantity)) {
            intents.remove(orderId);
            return false;
        }
        intents.markEscrowed(orderId);
        MarketOrder pendingOrder = new MarketOrder(orderId, player.getStringUUID(), commodity.copy(), quantity,
                pricePerUnit, true, player.getServer().overworld().getGameTime());
        FinancialSettlementJournalSavedData orderJournal = FinancialSettlementJournalSavedData.get(player.getServer());
        long orderTime = player.getServer().overworld().getGameTime();
        orderJournal.markStarted(orderId, "commodity", "order-record", quantity, orderTime);
        data.addOrder(pendingOrder);
        orderJournal.markCompleted(orderId, "commodity", "order-record", quantity, orderTime);
        intents.remove(orderId);

        int remaining = quantity;
        for (MarketOrder buy : crossingBuys(data, itemId, pricePerUnit, player.getStringUUID())) {
            if (remaining <= 0) {
                break;
            }
            int fill = Math.min(remaining, buy.quantity());
            long gross = EconomyMath.multiply(fill, buy.pricePerUnit());
            if (gross < 0) {
                break;
            }
            UUID buyerId = UUID.fromString(buy.ownerId());
            String tradeSource = "commodity-trade:" + orderId + ":" + buy.id()
                    + ":" + fill + ":" + gross;
            FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(player.getServer());
            long now = player.getServer().overworld().getGameTime();
            journal.markStarted(tradeSource, "commodity", "seller-payout", Money.toMinorSaturated(gross), now);
            if (!journal.isCompleted(tradeSource, "seller-payout")
                    && !payOrPend(player.getServer(), player, player.getUUID(), gross - commission(gross), tradeSource + ":money")) {
                break;
            }
            journal.markCompleted(tradeSource, "commodity", "seller-payout", Money.toMinorSaturated(gross), now);
            String goodsSource = tradeSource + ":goods";
            journal.markStarted(tradeSource, "commodity", "goods-delivery", fill, now);
            if (!journal.isCompleted(tradeSource, "goods-delivery")
                    && !warehouse.hasCreditSource(goodsSource)
                    && !warehouse.creditOnce(InventoryOwner.player(buyerId), commodity.getItem(), fill, goodsSource)) break;
            journal.markCompleted(tradeSource, "commodity", "goods-delivery", fill, now);
            journal.markStarted(tradeSource, "commodity", "tax", Money.toMinorSaturated(gross), now);
            TaxTransactionService.assess(player.getServer(), TaxType.VAT, player.getUUID(), Currencies.USD.id(),
                    Money.toMinorSaturated(gross), "commodity-sale:" + tradeSource, now);
            journal.markCompleted(tradeSource, "commodity", "tax", Money.toMinorSaturated(gross), now);
            String volumeSource = tradeSource + ":volume";
            if (!journal.isCompleted(tradeSource, "volume")) {
                if (!data.hasNetVolumeSource(volumeSource)) data.addNetVolumeOnce(itemId, -fill, volumeSource);
                journal.markCompleted(tradeSource, "commodity", "volume", fill, now);
            }
            remaining -= fill;
            reduceOrRemove(data, buy, fill);
            MarketOrder currentSell = data.findOrder(orderId);
            if (currentSell != null) {
                if (currentSell.quantity() <= fill) data.removeOrder(orderId);
                else data.replaceOrder(currentSell.withQuantity(currentSell.quantity() - fill));
            }
            NeoForge.EVENT_BUS.post(new TradeCompletedEvent(null, player, commodity, fill, "usd", gross,
                    "commodity", commission(gross)));
        }

        // The sell order was persisted before matching; each fill updates its
        // residual escrow quantity instead of creating it after the fact.
        data.setDirty();
        return true;
    }

    /** Restores escrowed commodity sell intents whose order record was interrupted. */
    public static int recoverPendingSellIntents(MinecraftServer server) {
        if (server == null) return 0;
        CommoditySavedData orders = CommoditySavedData.get(server);
        CommoditySellIntentSavedData intents = CommoditySellIntentSavedData.get(server);
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        int recovered = 0;
        for (CommoditySellIntentSavedData.Intent intent : intents.intents()) {
            if (orders.findOrder(intent.orderId()) != null) {
                intents.remove(intent.orderId());
                recovered++;
                continue;
            }
            ServerPlayer seller = server.getPlayerList().getPlayer(intent.sellerUuid());
            if (seller == null) continue;
            ItemStack item = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .get(net.minecraft.resources.ResourceLocation.parse(intent.itemId())));
            if (item.isEmpty()) continue;
            if (!intent.escrowed()) {
                long current = warehouse.count(seller.getUUID(), intent.itemId());
                long expectedAfter = intent.warehouseBefore() >= intent.quantity()
                        ? intent.warehouseBefore() - intent.quantity() : -1L;
                if (current == intent.warehouseBefore()) {
                    if (!warehouse.consume(seller.getUUID(), item.getItem(), intent.quantity())) continue;
                } else if (current != expectedAfter) {
                    continue;
                }
                intents.markEscrowed(intent.orderId());
            } else if (warehouse.count(seller.getUUID(), intent.itemId())
                    != intent.warehouseBefore() - intent.quantity()) {
                continue;
            }
            orders.addOrder(new MarketOrder(intent.orderId(), intent.sellerUuid().toString(), item,
                    intent.quantity(), intent.pricePerUnit(), true, intent.createdAt()));
            intents.remove(intent.orderId());
            recovered++;
        }
        return recovered;
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
        String orderId = UUID.randomUUID().toString();
        if (total < 0 || Money.toMinor(total) <= 0L) {
            return false;
        }
        CommodityBuyIntentSavedData intents = CommodityBuyIntentSavedData.get(player.getServer());
        intents.add(new CommodityBuyIntentSavedData.Intent(orderId, player.getUUID(), itemId, quantity,
                pricePerUnit, player.getServer().overworld().getGameTime(), false));
        String orderSource = "commodity-buy-order:" + orderId;
        if (!EconomyHelper.tryPayWithReference(player, Currencies.USD, Money.toMinor(total), orderSource)) {
            intents.remove(orderId);
            return false;
        }
        intents.markPaid(orderId);
        WarehouseSavedData warehouse = WarehouseSavedData.get(player.getServer());

        MarketOrder pendingOrder = new MarketOrder(orderId, player.getStringUUID(), commodity.copy(), quantity,
                pricePerUnit, false, player.getServer().overworld().getGameTime());
        FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(player.getServer());
        long settlementTime = player.getServer().overworld().getGameTime();
        journal.markStarted(orderId, "commodity", "order-record", quantity, settlementTime);
        data.addOrder(pendingOrder);
        journal.markCompleted(orderId, "commodity", "order-record", quantity, settlementTime);
        intents.remove(orderId);

        int remaining = quantity;
        long spent = 0L;
        for (MarketOrder sell : crossingSells(data, itemId, pricePerUnit, player.getStringUUID())) {
            if (remaining <= 0) {
                break;
            }
            int fill = Math.min(remaining, sell.quantity());
            long gross = EconomyMath.multiply(fill, sell.pricePerUnit());
            if (gross < 0) {
                break;
            }
            String tradeSource = "commodity-trade:" + orderId + ":" + sell.id()
                    + ":" + fill + ":" + gross;
            UUID sellerId = UUID.fromString(sell.ownerId());
            ServerPlayer seller = player.getServer().getPlayerList().getPlayer(sellerId);
            long now = player.getServer().overworld().getGameTime();
            journal.markStarted(tradeSource, "commodity", "seller-payout", Money.toMinorSaturated(gross), now);
            if (!journal.isCompleted(tradeSource, "seller-payout")
                    && !payOrPend(player.getServer(), seller, sellerId, gross - commission(gross), tradeSource + ":money")) {
                break;
            }
            journal.markCompleted(tradeSource, "commodity", "seller-payout", Money.toMinorSaturated(gross), now);
            String goodsSource = tradeSource + ":goods";
            journal.markStarted(tradeSource, "commodity", "goods-delivery", fill, now);
            if (!journal.isCompleted(tradeSource, "goods-delivery")
                    && !warehouse.hasCreditSource(goodsSource)
                    && !warehouse.creditOnce(InventoryOwner.player(player.getUUID()), commodity.getItem(), fill, goodsSource)) break;
            journal.markCompleted(tradeSource, "commodity", "goods-delivery", fill, now);
            journal.markStarted(tradeSource, "commodity", "tax", Money.toMinorSaturated(gross), now);
            TaxTransactionService.assess(player.getServer(), TaxType.VAT, sellerId, Currencies.USD.id(),
                    Money.toMinorSaturated(gross), "commodity-sale:" + tradeSource,
                    now);
            journal.markCompleted(tradeSource, "commodity", "tax", Money.toMinorSaturated(gross), now);
            String volumeSource = tradeSource + ":volume";
            if (!journal.isCompleted(tradeSource, "volume")) {
                if (!data.hasNetVolumeSource(volumeSource)) data.addNetVolumeOnce(itemId, fill, volumeSource);
                journal.markCompleted(tradeSource, "commodity", "volume", fill, now);
            }
            spent += gross;
            remaining -= fill;
            reduceOrRemove(data, sell, fill);
            MarketOrder currentBuy = data.findOrder(orderId);
            if (currentBuy != null) {
                if (currentBuy.quantity() <= fill) {
                    data.removeOrder(orderId);
                } else {
                    data.replaceOrder(currentBuy.withQuantity(currentBuy.quantity() - fill));
                }
            }
            NeoForge.EVENT_BUS.post(new TradeCompletedEvent(player, seller, commodity, fill, "usd", gross,
                    "commodity", commission(gross)));
        }

        // The buy order was persisted before matching. Update or remove that
        // escrow record as fills complete, so a restart never loses the paid
        // residual order.
        long reserved = EconomyMath.multiply(remaining, pricePerUnit);
        long refund = total - spent - reserved;
        if (refund > 0) {
            MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(player.getServer());
            mailbox.creditMoneyOnce(player.getUUID(), Currencies.USD.id(), Money.toMinor(refund),
                    "commodity-buy-residual-refund:" + orderId);
            mailbox.redeemMoneyOnly(player);
        }
        data.setDirty();
        return true;
    }

    /** Restores paid commodity buy intents whose order record was interrupted. */
    public static int recoverPendingBuyIntents(MinecraftServer server) {
        if (server == null) return 0;
        CommoditySavedData orders = CommoditySavedData.get(server);
        CommodityBuyIntentSavedData intents = CommodityBuyIntentSavedData.get(server);
        int recovered = 0;
        for (CommodityBuyIntentSavedData.Intent intent : intents.intents()) {
            if (orders.findOrder(intent.orderId()) != null) {
                intents.remove(intent.orderId());
                recovered++;
                continue;
            }
            ServerPlayer buyer = server.getPlayerList().getPlayer(intent.buyerUuid());
            if (buyer == null) continue;
            String orderSource = "commodity-buy-order:" + intent.orderId();
            long total = EconomyMath.multiply(intent.quantity(), intent.pricePerUnit());
            if (total < 0L || Money.toMinor(total) <= 0L) {
                intents.remove(intent.orderId());
                continue;
            }
            if (!intent.paid()) {
                if (!EconomyHelper.tryPayWithReference(buyer, Currencies.USD,
                        Money.toMinor(total), orderSource)) continue;
                intents.markPaid(intent.orderId());
            }
            ItemStack item = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .get(net.minecraft.resources.ResourceLocation.parse(intent.itemId())));
            if (item.isEmpty()) continue;
            orders.addOrder(new MarketOrder(intent.orderId(), intent.buyerUuid().toString(), item,
                    intent.quantity(), intent.pricePerUnit(), false, intent.createdAt()));
            intents.remove(intent.orderId());
            recovered++;
        }
        return recovered;
    }

    /** Cancels the player's own order, returning the escrowed commodity or money. */
    public static boolean cancelOrder(ServerPlayer player, String orderId) {
        CommoditySavedData data = CommoditySavedData.get(player.getServer());
        MarketOrder order = data.findOrder(orderId);
        if (order == null || !order.ownerId().equals(player.getStringUUID())) {
            return false;
        }
        if (order.sell()) {
            WarehouseSavedData.get(player.getServer()).creditOnce(InventoryOwner.player(player.getUUID()),
                    order.commodity().getItem(), order.quantity(), "commodity-order-cancel-item:" + order.id());
        } else {
            long total = EconomyMath.multiply(order.quantity(), order.pricePerUnit());
            if (total >= 0) {
                MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(player.getServer());
                String refundSource = "commodity-order-cancel-money:" + order.id();
                if (!mailbox.hasCreditSource(refundSource)
                        && !mailbox.creditMoneyOnce(player.getUUID(), Currencies.USD.id(), Money.toMinor(total), refundSource)) {
                    return false;
                }
                mailbox.redeemMoneyOnly(player);
            }
        }
        data.removeOrder(orderId);
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
            long householdDemand = PopulationSavedData.get(server).demandUnits(id, oldPrice);
            supply = householdDemand >= supply ? -Math.min(1_000_000L, householdDemand - supply) : supply - householdDemand;
            long newPrice = CommodityPriceEconomics.nextPrice(oldPrice, fundamental, netVolume, supply);
            newPrice = applyPriceLimit(data, id, newPrice);
            data.putPrice(id, newPrice);
            data.resetNetVolume(id);
            data.resetSupply(id);
            data.addCandle(id, new Candle(oldPrice, Math.max(oldPrice, newPrice), Math.min(oldPrice, newPrice), newPrice));
        }
        data.setDirty();
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

    /**
     * Releases escrow for newly-created orders that have reached their configured
     * lifetime. Legacy orders with no creation timestamp are retained so loading
     * older worlds cannot unexpectedly destroy an order.
     */
    public static void expireOrders(MinecraftServer server, long now) {
        int expiryDays = Config.COMMODITY_ORDER_EXPIRY_DAYS.get();
        if (expiryDays <= 0) return;
        long lifetime = com.ailudick.capitalismmod.calendar.PerpetualCalendar.ticksForDays(expiryDays);
        CommoditySavedData data = CommoditySavedData.get(server);
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        for (MarketOrder order : new ArrayList<>(data.orders())) {
            if (order.createdAt() <= 0L || now < order.createdAt()
                    || now - order.createdAt() < lifetime) {
                continue;
            }
            UUID owner;
            try {
                owner = UUID.fromString(order.ownerId());
            } catch (IllegalArgumentException | NullPointerException exception) {
                // Leave malformed records for the explicit admin repair command.
                continue;
            }
            if (order.sell()) {
                warehouse.creditOnce(InventoryOwner.player(owner), order.commodity().getItem(), order.quantity(),
                        "commodity-order-expiry-item:" + order.id());
            } else {
                long total = EconomyMath.multiply(order.quantity(), order.pricePerUnit());
                if (total > 0L) {
                    MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
                    String refundSource = "commodity-order-expiry-money:" + order.id();
                    if (!mailbox.hasCreditSource(refundSource)
                            && !mailbox.creditMoneyOnce(owner, Currencies.USD.id(), Money.toMinor(total), refundSource)) {
                        continue;
                    }
                }
            }
            data.removeOrder(order.id());
        }
    }

    // ---- matching internals ----

    /** Buy orders for {@code itemId} with bid ≥ {@code limit}, best (highest) bid first. */
    private static List<MarketOrder> crossingBuys(CommoditySavedData data, String itemId, long limit, String ownerId) {
        List<MarketOrder> result = new ArrayList<>();
        for (MarketOrder order : data.orders()) {
            if (!order.sell() && !order.ownerId().equals(ownerId)
                    && Commodities.id(order.commodity()).equals(itemId) && order.pricePerUnit() >= limit) {
                result.add(order);
            }
        }
        result.sort(Comparator.comparingLong(MarketOrder::pricePerUnit).reversed()
                .thenComparingLong(MarketOrder::createdAt)
                .thenComparing(MarketOrder::id));
        return result;
    }

    /** Sell orders for {@code itemId} with ask ≤ {@code limit}, best (lowest) ask first. */
    private static List<MarketOrder> crossingSells(CommoditySavedData data, String itemId, long limit, String ownerId) {
        List<MarketOrder> result = new ArrayList<>();
        for (MarketOrder order : data.orders()) {
            if (order.sell() && !order.ownerId().equals(ownerId)
                    && Commodities.id(order.commodity()).equals(itemId) && order.pricePerUnit() <= limit) {
                result.add(order);
            }
        }
        result.sort(Comparator.comparingLong(MarketOrder::pricePerUnit)
                .thenComparingLong(MarketOrder::createdAt)
                .thenComparing(MarketOrder::id));
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
    private static boolean payOrPend(MinecraftServer server, ServerPlayer recipient, UUID recipientId,
                                  long amount, String source) {
        if (amount == 0L) return true;
        if (server == null || recipientId == null || amount < 0L || source == null || source.isBlank()) return false;
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
        boolean credited = mailbox.hasCreditSource(source)
                || mailbox.creditMoneyOnce(recipientId, Currencies.USD.id(), Money.toMinor(amount), source);
        if (!credited) return false;
        if (recipient != null) mailbox.redeemMoneyOnly(recipient);
        return true;
    }
}
