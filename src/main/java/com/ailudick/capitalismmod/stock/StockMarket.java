package com.ailudick.capitalismmod.stock;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.event.TradeCompletedEvent;
import net.neoforged.neoforge.common.NeoForge;
import com.ailudick.capitalismmod.economy.EconomySavedData;
import com.ailudick.capitalismmod.economy.FinancialSettlementJournalSavedData;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.ailudick.capitalismmod.tax.TaxTransactionService;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
        StockSettlementSavedData settlements = StockSettlementSavedData.get(player.getServer());
        StockOrder order = data.findOrder(orderId);
        if (order == null || !order.ownerId().equals(player.getStringUUID())) {
            return false;
        }
        if (settlements.has(orderId)) {
            data.removeOrder(orderId);
            return true;
        }
        if (order.sell()) {
            String refundSource = "stock-order-cancel-shares:" + order.id();
            if (!data.hasShareCredit(refundSource)
                    && !data.addSharesOnce(order.stockId(), player.getUUID(), order.quantity(), refundSource)) {
                return false;
            }
        } else {
            long total = EconomyMath.multiply(order.quantity(), order.pricePerUnit());
            if (total >= 0) {
                MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(player.getServer());
                String refundSource = "stock-order-cancel-money:" + order.id();
                if (!mailbox.hasCreditSource(refundSource)
                        && !mailbox.creditMoneyOnce(player.getUUID(), Currencies.USD.id(), Money.toMinor(total), refundSource)) {
                    return false;
                }
                mailbox.redeemMoneyOnly(player);
            }
        }
        settlements.record(orderId);
        data.removeOrder(orderId);
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
        }
        data.setDirty();
    }

    /** Fast local animation for the built-in test stock used by the exchange UI. */
    public static void updateTestStock(MinecraftServer server) {
        EconomySavedData data = EconomySavedData.get(server);
        String id = "test_company";
        if (!Stocks.exists(id)) return;
        long oldPrice = Math.max(1L, data.price(id));
        long next = Math.max(1L, oldPrice + ThreadLocalRandom.current().nextLong(-3L, 4L));
        data.ensureStock(id, oldPrice);
        data.putPrice(id, next);
        data.addCandle(id, new Candle(oldPrice, Math.max(oldPrice, next),
                Math.min(oldPrice, next), next));
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

    /** Releases escrow for new stock orders that exceed the configured lifetime. */
    public static void expireOrders(MinecraftServer server, long now) {
        int expiryDays = Config.STOCK_ORDER_EXPIRY_DAYS.get();
        if (expiryDays <= 0) return;
        long lifetime = PerpetualCalendar.ticksForDays(expiryDays);
        EconomySavedData data = EconomySavedData.get(server);
        StockSettlementSavedData settlements = StockSettlementSavedData.get(server);
        for (StockOrder order : new ArrayList<>(data.orders())) {
            if (order.createdAt() <= 0L || now < order.createdAt()
                    || now - order.createdAt() < lifetime) {
                continue;
            }
            UUID owner;
            try {
                owner = UUID.fromString(order.ownerId());
            } catch (IllegalArgumentException | NullPointerException exception) {
                continue;
            }
            if (settlements.has(order.id())) {
                data.removeOrder(order.id());
                continue;
            }
            if (order.sell()) {
                String refundSource = "stock-order-expiry-shares:" + order.id();
                if (!data.hasShareCredit(refundSource)
                        && !data.addSharesOnce(order.stockId(), owner, order.quantity(), refundSource)) {
                    continue;
                }
            } else {
                long total = EconomyMath.multiply(order.quantity(), order.pricePerUnit());
                long minor = total > 0L ? Money.toMinor(total) : -1L;
                if (minor > 0L) {
                    MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
                    String refundSource = "stock-order-expiry-refund:" + order.id();
                    if (!mailbox.hasCreditSource(refundSource)
                            && !mailbox.creditMoneyOnce(owner, Currencies.USD.id(), minor, refundSource)) {
                        continue;
                    }
                }
            }
            settlements.record(order.id());
            data.removeOrder(order.id());
        }
    }

    // ---- matching internals ----

    private static boolean placeBuyOrder(ServerPlayer player, EconomySavedData data, String stockId, int quantity, long pricePerUnit) {
        String orderId = UUID.randomUUID().toString();
        long total = EconomyMath.multiply(quantity, pricePerUnit);
        if (total < 0 || Money.toMinor(total) <= 0L) {
            return false;
        }
        StockBuyIntentSavedData intents = StockBuyIntentSavedData.get(player.getServer());
        intents.add(new StockBuyIntentSavedData.Intent(orderId, player.getUUID(), stockId, quantity,
                pricePerUnit, player.getServer().overworld().getGameTime(), false));
        String orderSource = "stock-buy-order:" + orderId;
        if (!EconomyHelper.tryPayWithReference(player, Currencies.USD, Money.toMinor(total), orderSource)) {
            intents.remove(orderId);
            return false;
        }
        intents.markPaid(orderId);
        long orderTime = player.getServer().overworld().getGameTime();
        data.addOrder(new StockOrder(orderId, player.getStringUUID(), stockId, quantity, pricePerUnit,
                false, orderTime));
        intents.remove(orderId);
        int remaining = quantity;
        long spent = 0L;

        List<StockOrder> sells = crossingSells(data, stockId, pricePerUnit, player.getStringUUID());
        for (StockOrder sell : sells) {
            if (remaining <= 0) {
                break;
            }
            int fill = Math.min(remaining, sell.quantity());
            long gross = EconomyMath.multiply(fill, sell.pricePerUnit());
            if (gross < 0) {
                break;
            }
            String tradeSource = "stock-trade:" + orderId + ":" + sell.id()
                    + ":" + fill + ":" + gross;
            FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(player.getServer());
            long now = player.getServer().overworld().getGameTime();
            journal.markStarted(tradeSource, "stock", "shares", fill, now);
            String shareSource = tradeSource + ":shares";
            if (!data.hasShareCredit(shareSource)) data.addSharesOnce(stockId, player.getUUID(), fill, shareSource);
            if (!data.hasShareCredit(shareSource)) break;
            journal.markCompleted(tradeSource, "stock", "shares", fill, now);
            settleStampDuty(player.getServer(), UUID.fromString(sell.ownerId()), gross, tradeSource);
            if (!payTo(player.getServer(), UUID.fromString(sell.ownerId()),
                    Money.toMinor(gross - duty(gross)), tradeSource)) {
                break;
            }
            data.addNetVolumeOnce(stockId, fill, tradeSource);
            spent += gross;
            remaining -= fill;
            reduceOrRemove(data, sell, fill);
            StockOrder currentBuy = data.findOrder(orderId);
            if (currentBuy != null) {
                if (currentBuy.quantity() <= fill) data.removeOrder(orderId);
                else data.replaceOrder(currentBuy.withQuantity(currentBuy.quantity() - fill));
            }
            NeoForge.EVENT_BUS.post(new TradeCompletedEvent(player, null, null,
                    fill, "usd", gross, "stock", duty(gross), stockId));
        }

        long reserved = EconomyMath.multiply(remaining, pricePerUnit);
        long refund = total - spent - reserved;
        if (refund > 0) {
            MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(player.getServer());
            mailbox.creditMoneyOnce(player.getUUID(), Currencies.USD.id(), Money.toMinor(refund),
                    "stock-buy-residual-refund:" + orderId);
            mailbox.redeemMoneyOnly(player);
        }
        return true;
    }

    /** Restores paid stock buy intents whose order record was interrupted. */
    public static int recoverPendingBuyIntents(MinecraftServer server) {
        if (server == null) return 0;
        EconomySavedData data = EconomySavedData.get(server);
        StockBuyIntentSavedData intents = StockBuyIntentSavedData.get(server);
        int recovered = 0;
        for (StockBuyIntentSavedData.Intent intent : intents.intents()) {
            if (data.findOrder(intent.orderId()) != null) {
                intents.remove(intent.orderId());
                recovered++;
                continue;
            }
            ServerPlayer buyer = server.getPlayerList().getPlayer(intent.buyerUuid());
            if (buyer == null) continue;
            long total = EconomyMath.multiply(intent.quantity(), intent.pricePerUnit());
            if (total < 0L || Money.toMinor(total) <= 0L) {
                intents.remove(intent.orderId());
                continue;
            }
            if (!intent.paid() && !EconomyHelper.tryPayWithReference(buyer, Currencies.USD,
                    Money.toMinor(total), "stock-buy-order:" + intent.orderId())) continue;
            if (!intent.paid()) intents.markPaid(intent.orderId());
            data.addOrder(new StockOrder(intent.orderId(), intent.buyerUuid().toString(), intent.stockId(),
                    intent.quantity(), intent.pricePerUnit(), false, intent.createdAt()));
            intents.remove(intent.orderId());
            recovered++;
        }
        return recovered;
    }

    private static boolean placeSellOrder(ServerPlayer player, EconomySavedData data, String stockId, int quantity, long pricePerUnit) {
        if (data.holdings(stockId, player.getUUID()) < quantity) {
            return false;
        }
        String orderId = UUID.randomUUID().toString();
        StockSellIntentSavedData intents = StockSellIntentSavedData.get(player.getServer());
        intents.add(new StockSellIntentSavedData.Intent(orderId, player.getUUID(), stockId, quantity,
                pricePerUnit, player.getServer().overworld().getGameTime(),
                data.holdings(stockId, player.getUUID()), false));
        data.addShares(stockId, player.getUUID(), -quantity);
        intents.markSharesEscrowed(orderId);
        long orderTime = player.getServer().overworld().getGameTime();
        data.addOrder(new StockOrder(orderId, player.getStringUUID(), stockId, quantity, pricePerUnit,
                true, orderTime));
        intents.remove(orderId);
        int remaining = quantity;

        List<StockOrder> buys = crossingBuys(data, stockId, pricePerUnit, player.getStringUUID());
        for (StockOrder buy : buys) {
            if (remaining <= 0) {
                break;
            }
            int fill = Math.min(remaining, buy.quantity());
            long gross = EconomyMath.multiply(fill, buy.pricePerUnit());
            if (gross < 0) {
                break;
            }
            String tradeSource = "stock-trade:" + orderId + ":" + buy.id()
                    + ":" + fill + ":" + gross;
            FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(player.getServer());
            long now = player.getServer().overworld().getGameTime();
            journal.markStarted(tradeSource, "stock", "shares", fill, now);
            String shareSource = tradeSource + ":shares";
            if (!data.hasShareCredit(shareSource)) data.addSharesOnce(stockId, UUID.fromString(buy.ownerId()), fill, shareSource);
            if (!data.hasShareCredit(shareSource)) break;
            journal.markCompleted(tradeSource, "stock", "shares", fill, now);
            settleStampDuty(player.getServer(), player.getUUID(), gross, tradeSource);
            if (!payTo(player.getServer(), player.getUUID(), Money.toMinor(gross - duty(gross)),
                    tradeSource)) {
                break;
            }
            data.addNetVolumeOnce(stockId, -fill, tradeSource);
            remaining -= fill;
            reduceOrRemove(data, buy, fill);
            StockOrder currentSell = data.findOrder(orderId);
            if (currentSell != null) {
                if (currentSell.quantity() <= fill) data.removeOrder(orderId);
                else data.replaceOrder(currentSell.withQuantity(currentSell.quantity() - fill));
            }
            NeoForge.EVENT_BUS.post(new TradeCompletedEvent(null, player, null,
                    fill, "usd", gross, "stock", duty(gross), stockId));
        }

        return true;
    }

    /** Restores escrowed stock sell intents whose order record was interrupted. */
    public static int recoverPendingSellIntents(MinecraftServer server) {
        if (server == null) return 0;
        EconomySavedData data = EconomySavedData.get(server);
        StockSellIntentSavedData intents = StockSellIntentSavedData.get(server);
        int recovered = 0;
        for (StockSellIntentSavedData.Intent intent : intents.intents()) {
            if (data.findOrder(intent.orderId()) != null) {
                intents.remove(intent.orderId());
                recovered++;
                continue;
            }
            ServerPlayer seller = server.getPlayerList().getPlayer(intent.sellerUuid());
            if (seller == null) continue;
            if (!intent.sharesEscrowed()) {
                long currentHoldings = data.holdings(intent.stockId(), seller.getUUID());
                long expectedAfter = intent.holdingsBefore() >= intent.quantity()
                        ? intent.holdingsBefore() - intent.quantity() : -1L;
                if (currentHoldings == intent.holdingsBefore()) {
                    data.addShares(intent.stockId(), seller.getUUID(), -intent.quantity());
                } else if (currentHoldings != expectedAfter) {
                    continue;
                }
                intents.markSharesEscrowed(intent.orderId());
            }
            data.addOrder(new StockOrder(intent.orderId(), intent.sellerUuid().toString(), intent.stockId(),
                    intent.quantity(), intent.pricePerUnit(), true, intent.createdAt()));
            intents.remove(intent.orderId());
            recovered++;
        }
        return recovered;
    }

    /** Sell orders for {@code stockId} with ask ≤ {@code limit}, best (lowest) ask first. */
    private static List<StockOrder> crossingSells(EconomySavedData data, String stockId, long limit, String ownerId) {
        List<StockOrder> result = new ArrayList<>();
        for (StockOrder order : data.orders()) {
            if (order.stockId().equals(stockId) && order.sell() && !order.ownerId().equals(ownerId)
                    && order.pricePerUnit() <= limit) {
                result.add(order);
            }
        }
        result.sort(Comparator.comparingLong(StockOrder::pricePerUnit)
                .thenComparingLong(StockOrder::createdAt)
                .thenComparing(StockOrder::id));
        return result;
    }

    /** Buy orders for {@code stockId} with bid ≥ {@code limit}, best (highest) bid first. */
    private static List<StockOrder> crossingBuys(EconomySavedData data, String stockId, long limit, String ownerId) {
        List<StockOrder> result = new ArrayList<>();
        for (StockOrder order : data.orders()) {
            if (order.stockId().equals(stockId) && !order.sell() && !order.ownerId().equals(ownerId)
                    && order.pricePerUnit() >= limit) {
                result.add(order);
            }
        }
        result.sort(Comparator.comparingLong(StockOrder::pricePerUnit).reversed()
                .thenComparingLong(StockOrder::createdAt)
                .thenComparing(StockOrder::id));
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

    /** Withholds the stamp duty from sale proceeds and closes the matching tax bill. */
    private static void settleStampDuty(MinecraftServer server, UUID taxpayer, long gross, String tradeSource) {
        if (server == null || taxpayer == null || gross <= 0L) return;
        long now = server.overworld().getGameTime();
        if (tradeSource == null || tradeSource.isBlank()) return;
        String source = "stock-sale:" + tradeSource;
        FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(server);
        journal.markStarted(tradeSource, "stock", "tax", Money.toMinorSaturated(gross), now);
        TaxTransactionService.assess(server, TaxType.STAMP_DUTY, taxpayer, Currencies.USD.id(),
                Money.toMinorSaturated(gross), source, now);
        TaxService.settleFromProceeds(server,
                new TaxSubject(TaxType.STAMP_DUTY, source, taxpayer),
                Money.toMinorSaturated(duty(gross)), source + ":withheld", now);
        journal.markCompleted(tradeSource, "stock", "tax", Money.toMinorSaturated(gross), now);
    }

    private static boolean withinLimit(EconomySavedData data, String stockId, long price) {
        long prevClose = data.prevClose(stockId);
        if (prevClose <= 0) {
            return true;
        }
        double limit = Config.STOCK_PRICE_LIMIT.get();
        long lower = (long) (prevClose * (1 - limit));
        long upper = (long) (prevClose * (1 + limit));
        return price >= lower && price <= upper;
    }

    private static long applyPriceLimit(EconomySavedData data, String stockId, long newPrice) {
        long prevClose = data.prevClose(stockId);
        if (prevClose <= 0) {
            return newPrice;
        }
        double limit = Config.STOCK_PRICE_LIMIT.get();
        long lower = (long) (prevClose * (1 - limit));
        long upper = (long) (prevClose * (1 + limit));
        return Math.max(lower, Math.min(newPrice, upper));
    }

    /** Pays {@code amount} USD to {@code recipientId}, or parks it in the mailbox if offline. */
    private static boolean payTo(MinecraftServer server, UUID recipientId, long amount, String source) {
        if (server == null || recipientId == null || amount < 0L || source == null || source.isBlank()) return false;
        if (amount == 0L) return true;
        FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(server);
        long now = server.overworld().getGameTime();
        journal.markStarted(source, "stock", "seller-payout", amount, now);
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
        boolean credited = mailbox.hasCreditSource(source)
                || mailbox.creditMoneyOnce(recipientId, Currencies.USD.id(), amount, source);
        if (!credited) return false;
        ServerPlayer recipient = server.getPlayerList().getPlayer(recipientId);
        if (recipient != null) mailbox.redeemMoneyOnly(recipient);
        journal.markCompleted(source, "stock", "seller-payout", amount, now);
        return true;
    }
}
