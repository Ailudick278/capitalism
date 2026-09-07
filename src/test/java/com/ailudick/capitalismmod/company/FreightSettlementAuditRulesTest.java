package com.ailudick.capitalismmod.company;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreightSettlementAuditRulesTest {
    @Test
    void validatesCostAndQuote() {
        assertTrue(FreightSettlementAuditRules.validCost("s-1", "buyer", "minecraft:iron_ingot",
                4, 100L, 10L, true, "carrier", 20L));
        assertTrue(FreightSettlementAuditRules.quoteMatchesCost(100L, 100L));
        assertFalse(FreightSettlementAuditRules.quoteMatchesCost(100L, 90L));
    }

    @Test
    void settlementPhasesAreMonotonic() {
        assertTrue(FreightSettlementAuditRules.validSettlement("s-1", "buyer", "carrier",
                100L, true, true, true));
        assertFalse(FreightSettlementAuditRules.validSettlement("s-1", "buyer", "carrier",
                100L, false, true, false));
        assertFalse(FreightSettlementAuditRules.validSettlement("s-1", "buyer", "carrier",
                100L, true, false, true));
    }
}
