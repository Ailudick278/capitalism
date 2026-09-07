package com.ailudick.capitalismmod.stock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockOrderAuditRulesTest {
    @Test
    void validatesOrderAndEscrowBounds() {
        assertTrue(StockOrderAuditRules.valid("order-1", "player-1", "stock-1", 4, 12L, 20L));
        assertFalse(StockOrderAuditRules.valid("", "player-1", "stock-1", 4, 12L, 20L));
        assertTrue(StockOrderAuditRules.coversSellEscrow(10L, 4));
        assertFalse(StockOrderAuditRules.coversSellEscrow(3L, 4));
    }
}
