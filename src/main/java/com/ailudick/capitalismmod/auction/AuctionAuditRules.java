package com.ailudick.capitalismmod.auction;

import java.util.UUID;

/** Pure invariants for active auction records and listing recovery intents. */
public final class AuctionAuditRules {
    private AuctionAuditRules() {
    }

    public static boolean validAuction(String id, UUID seller, String itemId, int quantity,
                                       long startingPrice, long currentBid, String currentBidder, long endTick) {
        if (id == null || id.isBlank() || seller == null || itemId == null || itemId.isBlank()
                || quantity <= 0 || startingPrice <= 0L || currentBid < 0L || endTick < 0L
                || currentBidder == null) return false;
        if (currentBidder.isBlank()) return currentBid == 0L;
        try {
            UUID bidder = UUID.fromString(currentBidder);
            return !seller.equals(bidder) && currentBid >= startingPrice;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static boolean validListingIntent(String auctionId, UUID seller, String itemId, int quantity,
                                             long startingPrice, long endTick, long warehouseBefore) {
        return auctionId != null && !auctionId.isBlank() && seller != null
                && itemId != null && !itemId.isBlank() && quantity > 0 && startingPrice > 0L
                && endTick >= 0L && warehouseBefore >= quantity;
    }
}
