package com.ailudick.capitalismmod.auction;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.economy.FinancialSettlementJournalSavedData;
import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.ailudick.capitalismmod.tax.TaxTransactionService;
import com.ailudick.capitalismmod.tax.TaxType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Server-side auction house: list items for auction, bid, and settle expiring auctions.
 * Bids are paid up front and refunded when outbid. Items are escrowed from the delivery warehouse.
 */
public final class AuctionMarket {
    private AuctionMarket() {
    }

    /** Lists {@code quantity} of a commodity for auction, escrowing it from the warehouse. */
    public static boolean listAuction(ServerPlayer player, int commodityIndex, int quantity, long startingPrice, int durationSeconds) {
        if (!Commodities.isValid(commodityIndex) || quantity <= 0 || startingPrice <= 0 || durationSeconds <= 0) {
            return false;
        }
        Item item = Commodities.get(commodityIndex).getItem();
        String itemId = Commodities.id(Commodities.get(commodityIndex));
        long durationTicks;
        long endTick;
        try {
            durationTicks = Math.multiplyExact((long) durationSeconds, 20L);
            endTick = Math.addExact(player.getServer().overworld().getGameTime(), durationTicks);
        } catch (ArithmeticException exception) {
            return false;
        }
        // Validate the complete listing before escrow so an invalid duration
        // cannot remove goods without creating a recoverable auction record.
        WarehouseSavedData warehouse = WarehouseSavedData.get(player.getServer());
        String auctionId = UUID.randomUUID().toString();
        AuctionListingIntentSavedData intents = AuctionListingIntentSavedData.get(player.getServer());
        intents.add(new AuctionListingIntentSavedData.Intent(auctionId, player.getUUID(), itemId, quantity,
                startingPrice, endTick, warehouse.count(player.getUUID(), itemId), false));
        if (!warehouse.consume(player.getUUID(), item, quantity)) {
            intents.remove(auctionId);
            return false;
        }
        intents.markEscrowed(auctionId);
        AuctionSavedData.get(player.getServer()).addAuction(new Auction(
                auctionId, player.getUUID(), itemId, quantity, startingPrice, 0L, "", endTick));
        intents.remove(auctionId);
        return true;
    }

    /** Restores escrowed auction listings whose auction record was interrupted. */
    public static int recoverListingIntents(MinecraftServer server) {
        if (server == null) return 0;
        AuctionSavedData auctions = AuctionSavedData.get(server);
        AuctionListingIntentSavedData intents = AuctionListingIntentSavedData.get(server);
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        int recovered = 0;
        for (AuctionListingIntentSavedData.Intent intent : intents.intents()) {
            if (auctions.findAuction(intent.auctionId()) != null) {
                intents.remove(intent.auctionId());
                recovered++;
                continue;
            }
            ServerPlayer seller = server.getPlayerList().getPlayer(intent.seller());
            if (seller == null) continue;
            long current = warehouse.count(seller.getUUID(), intent.itemId());
            long expectedAfter = intent.warehouseBefore() >= intent.quantity()
                    ? intent.warehouseBefore() - intent.quantity() : -1L;
            if (!intent.escrowed()) {
                if (current == intent.warehouseBefore()) {
                    var commodity = Commodities.byId(intent.itemId());
                    if (commodity == null || !warehouse.consume(seller.getUUID(), commodity.getItem(), intent.quantity())) continue;
                } else if (current != expectedAfter) {
                    continue;
                }
                intents.markEscrowed(intent.auctionId());
            } else if (current != expectedAfter) {
                continue;
            }
            auctions.addAuction(new Auction(intent.auctionId(), intent.seller(), intent.itemId(), intent.quantity(),
                    intent.startingPrice(), 0L, "", intent.endTick()));
            intents.remove(intent.auctionId());
            recovered++;
        }
        return recovered;
    }

    /** Places a bid on an auction, refunding the previous high bidder. */
    public static boolean bid(ServerPlayer player, String auctionId, long amount) {
        AuctionSavedData data = AuctionSavedData.get(player.getServer());
        AuctionBidSavedData bidJournal = AuctionBidSavedData.get(player.getServer());
        Auction auction = data.findAuction(auctionId);
        if (auction == null || auction.endTick() <= player.getServer().overworld().getGameTime()
                || auction.seller().equals(player.getUUID())) {
            return false;
        }
        if (amount < auction.startingPrice() || amount <= auction.currentBid()) {
            return false;
        }
        UUID previousBidder = null;
        long previousBidMinor = 0L;
        if (!auction.currentBidder().isEmpty()) {
            try {
                previousBidder = UUID.fromString(auction.currentBidder());
            } catch (IllegalArgumentException exception) {
                return false;
            }
            previousBidMinor = Money.toMinor(auction.currentBid());
            if (previousBidMinor <= 0L) return false;
        }
        AuctionBidSavedData.Bid recorded = bidJournal.find(auctionId, player.getUUID(), amount);
        if (recorded != null) {
            data.replaceAuction(auction.withBid(amount, player.getStringUUID()));
            return true;
        }
        long bidMinor = Money.toMinor(amount);
        String paymentReference = "auction-bid:" + auction.id() + ":" + player.getUUID() + ":" + amount;
        if (bidMinor <= 0L || !EconomyHelper.tryPayWithReference(player, Currencies.USD, bidMinor,
                paymentReference)) {
            return false;
        }
        if (!auction.currentBidder().isEmpty()) {
            String refundSource = "auction-outbid:" + auction.id() + ":" + previousBidder
                    + ":" + previousBidMinor;
            MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(player.getServer());
            if (!mailbox.creditMoneyOnce(previousBidder, Currencies.USD.id(), previousBidMinor, refundSource)) {
                // A previous attempt already recorded this refund. Redeeming the
                // mailbox below is still safe and lets an online bidder receive it.
            }
            ServerPlayer prev = player.getServer().getPlayerList().getPlayer(previousBidder);
            if (prev != null) mailbox.redeemMoneyOnly(prev);
        }
        bidJournal.record(new AuctionBidSavedData.Bid(auctionId, player.getUUID(), amount));
        data.replaceAuction(auction.withBid(amount, player.getStringUUID()));
        return true;
    }

    /** Cancels the seller's auction only while it has no bids, returning escrowed goods. */
    public static boolean cancelAuction(ServerPlayer player, String auctionId) {
        if (player == null || auctionId == null || auctionId.isBlank()) return false;
        AuctionSavedData data = AuctionSavedData.get(player.getServer());
        AuctionSettlementSavedData settlements = AuctionSettlementSavedData.get(player.getServer());
        Auction auction = data.findAuction(auctionId);
        if (auction == null || !auction.seller().equals(player.getUUID())
                || !auction.currentBidder().isEmpty()) return false;
        var commodity = Commodities.byId(auction.itemId());
        if (commodity == null || auction.quantity() <= 0) return false;
        if (settlements.has(auction.id())) {
            data.removeAuction(auction.id());
            return true;
        }
        WarehouseSavedData.get(player.getServer()).creditOnce(InventoryOwner.player(player.getUUID()),
                commodity.getItem(), auction.quantity(), "auction-cancel-item:" + auction.id());
        settlements.record(auction.id());
        data.removeAuction(auction.id());
        return true;
    }

    /** Settles all auctions whose end time has passed. */
    public static void settleExpired(MinecraftServer server) {
        AuctionSavedData data = AuctionSavedData.get(server);
        AuctionSettlementSavedData settlements = AuctionSettlementSavedData.get(server);
        long now = server.overworld().getGameTime();
        for (Auction auction : new ArrayList<>(data.auctions())) {
            if (auction.endTick() > now) {
                continue;
            }
            if (settlements.has(auction.id())) {
                data.removeAuction(auction.id());
                continue;
            }
            Item item = Commodities.byId(auction.itemId()) == null ? null : Commodities.byId(auction.itemId()).getItem();
            if (item == null) {
                CapitalismMod.LOGGER.warn("Keeping auction {} active because item {} no longer resolves; use /marketrepair.",
                        auction.id(), auction.itemId());
                continue;
            }
            WarehouseSavedData warehouse = WarehouseSavedData.get(server);
            FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(server);
            String transactionId = "auction:" + auction.id();
            long settlementTime = server.overworld().getGameTime();
            if (auction.currentBidder().isEmpty()) {
                journal.markStarted(transactionId, "auction", "goods-return", auction.quantity(), settlementTime);
                warehouse.creditOnce(InventoryOwner.player(auction.seller()), item, auction.quantity(),
                        "auction-item:" + auction.id());
                journal.markCompleted(transactionId, "auction", "goods-return", auction.quantity(), settlementTime);
            } else {
                UUID winner;
                try {
                    winner = UUID.fromString(auction.currentBidder());
                } catch (IllegalArgumentException exception) {
                    CapitalismMod.LOGGER.warn("Keeping auction {} active because bidder {} is invalid; use /marketrepair.",
                            auction.id(), auction.currentBidder());
                    continue;
                }
                long bidMinor = Money.toMinor(auction.currentBid());
                if (bidMinor <= 0L) {
                    CapitalismMod.LOGGER.warn("Keeping auction {} active because bid {} is invalid; use /marketrepair.",
                            auction.id(), auction.currentBid());
                    continue;
                }
                journal.markStarted(transactionId, "auction", "goods-delivery", auction.quantity(), settlementTime);
                warehouse.creditOnce(InventoryOwner.player(winner), item, auction.quantity(),
                        "auction-item:" + auction.id());
                journal.markCompleted(transactionId, "auction", "goods-delivery", auction.quantity(), settlementTime);
                String payoutSource = "auction-payout:" + auction.id();
                MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
                journal.markStarted(transactionId, "auction", "seller-payout", bidMinor, settlementTime);
                if (!mailbox.hasCreditSource(payoutSource)
                        && !mailbox.creditMoneyOnce(auction.seller(), Currencies.USD.id(), bidMinor, payoutSource)) continue;
                journal.markCompleted(transactionId, "auction", "seller-payout", bidMinor, settlementTime);
                journal.markStarted(transactionId, "auction", "sale-tax", Money.toMinorSaturated(auction.currentBid()), settlementTime);
                TaxTransactionService.assess(server, TaxType.VAT, auction.seller(), Currencies.USD.id(),
                        Money.toMinorSaturated(auction.currentBid()), "auction-sale:" + auction.id(),
                        server.overworld().getGameTime());
                journal.markCompleted(transactionId, "auction", "sale-tax", Money.toMinorSaturated(auction.currentBid()), settlementTime);
                ServerPlayer seller = server.getPlayerList().getPlayer(auction.seller());
                settlements.record(auction.id());
                data.removeAuction(auction.id());
                if (seller != null) mailbox.redeemMoneyOnly(seller);
                continue;
            }
            settlements.record(auction.id());
            data.removeAuction(auction.id());
        }
    }
}
