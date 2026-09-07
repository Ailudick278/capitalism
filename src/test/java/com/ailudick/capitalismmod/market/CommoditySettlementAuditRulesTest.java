package com.ailudick.capitalismmod.market;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommoditySettlementAuditRulesTest {
    @Test
    void requiresEveryCommodityFillPhase() {
        assertTrue(CommoditySettlementAuditRules.complete(Set.of(
                "seller-payout", "goods-delivery", "tax", "volume", "order-update")));
        assertFalse(CommoditySettlementAuditRules.complete(Set.of(
                "seller-payout", "goods-delivery", "tax", "volume")));
    }
}
