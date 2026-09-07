package com.ailudick.capitalismmod.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommodityFlowAuditRulesTest {
    @Test
    void acceptsSignedDailyFlowWithinOperationalBounds() {
        assertTrue(CommodityFlowAuditRules.valid("minecraft:wheat", 1_000L));
        assertTrue(CommodityFlowAuditRules.valid("minecraft:wheat", -1_000L));
    }

    @Test
    void rejectsMalformedOrOverflowedFlow() {
        assertFalse(CommodityFlowAuditRules.valid("", 0L));
        assertFalse(CommodityFlowAuditRules.valid("minecraft:wheat", 1_000_000_000_001L));
        assertFalse(CommodityFlowAuditRules.valid("minecraft:wheat", -1_000_000_000_001L));
    }
}
