package com.ailudick.capitalismmod.auction;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionAuditRulesTest {
    private static final UUID SELLER = UUID.randomUUID();
    private static final UUID BIDDER = UUID.randomUUID();

    @Test
    void validatesActiveAuctionBidState() {
        assertTrue(AuctionAuditRules.validAuction("auction-1", SELLER, "minecraft:wheat",
                4, 10L, 0L, "", 100L));
        assertTrue(AuctionAuditRules.validAuction("auction-1", SELLER, "minecraft:wheat",
                4, 10L, 12L, BIDDER.toString(), 100L));
        assertFalse(AuctionAuditRules.validAuction("auction-1", SELLER, "minecraft:wheat",
                4, 10L, 12L, SELLER.toString(), 100L));
    }

    @Test
    void requiresListingWarehouseBeforeQuantity() {
        assertTrue(AuctionAuditRules.validListingIntent("auction-1", SELLER, "minecraft:wheat",
                4, 10L, 100L, 8L));
        assertFalse(AuctionAuditRules.validListingIntent("auction-1", SELLER, "minecraft:wheat",
                4, 10L, 100L, 3L));
    }
}
