package com.ailudick.capitalismmod.stock;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockSettlementAuditRulesTest {
    @Test
    void requiresEveryStockFillPhase() {
        assertTrue(StockSettlementAuditRules.complete(Set.of(
                "shares", "tax", "seller-payout", "volume", "order-update")));
        assertFalse(StockSettlementAuditRules.complete(Set.of(
                "shares", "tax", "seller-payout", "order-update")));
    }
}
