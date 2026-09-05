package com.ailudick.capitalismmod.auction;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
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
        if (!WarehouseSavedData.get(player.getServer()).consume(player.getUUID(), item, quantity)) {
            return false;
        }
        AuctionSavedData.get(player.getServer()).addAuction(new Auction(
                UUID.randomUUID().toString(), player.getUUID(), itemId, quantity, startingPrice, 0L, "", endTick));
        return true;
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
        AuctionBidSavedData.Bid recorded = bidJournal.find(auctionId, player.getUUID(), amount);
        if (recorded != null) {
            data.replaceAuction(auction.withBid(amount, player.getStringUUID()));
            return true;
        }
        long bidMinor = Money.toMinor(amount);
        if (bidMinor <= 0L || !EconomyHelper.tryPay(player, Currencies.USD, bidMinor)) {
            return false;
        }
        if (!auction.currentBidder().isEmpty()) {
            UUID prevBidder;
            try {
                prevBidder = UUID.fromString(auction.currentBidder());
            } catch (IllegalArgumentException exception) {
                // Do not accept a new bid while an inconsistent escrow owner needs repair.
                EconomyHelper.giveMoney(player, Currencies.USD, bidMinor);
                return false;
            }
            long previousBidMinor = Money.toMinor(auction.currentBid());
            if (previousBidMinor <= 0L) {
                EconomyHelper.giveMoney(player, Currencies.USD, bidMinor);
                return false;
            }
            ServerPlayer prev = player.getServer().getPlayerList().getPlayer(prevBidder);
            if (prev != null) {
                EconomyHelper.giveMoney(prev, Currencies.USD, previousBidMinor);
            } else {
                MarketMailboxSavedData.get(player.getServer()).creditMoney(prevBidder, "usd", previousBidMinor);
            }
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
        WarehouseSavedData.get(player.getServer()).credit(player.getUUID(), commodity.getItem(), auction.quantity());
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
            if (auction.currentBidder().isEmpty()) {
                warehouse.credit(auction.seller(), item, auction.quantity());
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
                warehouse.credit(winner, item, auction.quantity());
                ServerPlayer seller = server.getPlayerList().getPlayer(auction.seller());
                if (seller != null) {
                    EconomyHelper.giveMoney(seller, Currencies.USD, bidMinor);
                } else {
                    MarketMailboxSavedData.get(server).creditMoney(auction.seller(), "usd", bidMinor);
                }
                TaxTransactionService.assess(server, TaxType.VAT, auction.seller(), Currencies.USD.id(),
                        Money.toMinorSaturated(auction.currentBid()), "auction-sale:" + auction.id(),
                        server.overworld().getGameTime());
            }
            settlements.record(auction.id());
            data.removeAuction(auction.id());
        }
    }
}
