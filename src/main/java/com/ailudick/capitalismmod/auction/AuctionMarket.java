package com.ailudick.capitalismmod.auction;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
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
        if (!WarehouseSavedData.get(player.getServer()).consume(player.getUUID(), item, quantity)) {
            return false;
        }
        long endTick = player.getServer().getTickCount() + (long) durationSeconds * 20L;
        AuctionSavedData.get(player.getServer()).addAuction(new Auction(
                UUID.randomUUID().toString(), player.getUUID(), itemId, quantity, startingPrice, 0L, "", endTick));
        return true;
    }

    /** Places a bid on an auction, refunding the previous high bidder. */
    public static boolean bid(ServerPlayer player, String auctionId, long amount) {
        AuctionSavedData data = AuctionSavedData.get(player.getServer());
        Auction auction = data.findAuction(auctionId);
        if (auction == null || auction.endTick() <= player.getServer().getTickCount()) {
            return false;
        }
        if (amount < auction.startingPrice() || amount <= auction.currentBid()) {
            return false;
        }
        if (!EconomyHelper.tryPay(player, Currencies.USD, Money.toMinor(amount))) {
            return false;
        }
        if (!auction.currentBidder().isEmpty()) {
            UUID prevBidder = UUID.fromString(auction.currentBidder());
            ServerPlayer prev = player.getServer().getPlayerList().getPlayer(prevBidder);
            if (prev != null) {
                EconomyHelper.giveMoney(prev, Currencies.USD, Money.toMinor(auction.currentBid()));
            } else {
                MarketMailboxSavedData.get(player.getServer()).creditMoney(prevBidder, "usd", Money.toMinor(auction.currentBid()));
            }
        }
        data.replaceAuction(auction.withBid(amount, player.getStringUUID()));
        return true;
    }

    /** Settles all auctions whose end time has passed. */
    public static void settleExpired(MinecraftServer server) {
        AuctionSavedData data = AuctionSavedData.get(server);
        long now = server.getTickCount();
        for (Auction auction : new ArrayList<>(data.auctions())) {
            if (auction.endTick() > now) {
                continue;
            }
            Item item = Commodities.byId(auction.itemId()) == null ? null : Commodities.byId(auction.itemId()).getItem();
            if (item == null) {
                data.removeAuction(auction.id());
                continue;
            }
            WarehouseSavedData warehouse = WarehouseSavedData.get(server);
            if (auction.currentBidder().isEmpty()) {
                warehouse.credit(auction.seller(), item, auction.quantity());
            } else {
                warehouse.credit(UUID.fromString(auction.currentBidder()), item, auction.quantity());
                ServerPlayer seller = server.getPlayerList().getPlayer(auction.seller());
                if (seller != null) {
                    EconomyHelper.giveMoney(seller, Currencies.USD, Money.toMinor(auction.currentBid()));
                } else {
                    MarketMailboxSavedData.get(server).creditMoney(auction.seller(), "usd", Money.toMinor(auction.currentBid()));
                }
            }
            data.removeAuction(auction.id());
        }
    }
}
