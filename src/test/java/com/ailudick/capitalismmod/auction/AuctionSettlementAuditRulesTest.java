package com.ailudick.capitalismmod.auction;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionSettlementAuditRulesTest {
    @Test
    void acceptsReturnOrCompleteSaleOutcome() {
        assertTrue(AuctionSettlementAuditRules.hasCompleteOutcome(Set.of("goods-return")));
        assertTrue(AuctionSettlementAuditRules.hasCompleteOutcome(Set.of(
                "goods-delivery", "seller-payout", "sale-tax")));
        assertFalse(AuctionSettlementAuditRules.hasCompleteOutcome(Set.of(
                "goods-delivery", "seller-payout")));
    }
}
