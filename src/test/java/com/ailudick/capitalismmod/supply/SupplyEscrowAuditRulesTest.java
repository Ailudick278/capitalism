package com.ailudick.capitalismmod.supply;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplyEscrowAuditRulesTest {
    @Test
    void acceptsOnlyMatchingRetryParameters() {
        var current = new SupplyEscrowSavedData.Escrow("order-1", 10, 1_000L,
                400L, 600L, 0L);
        assertTrue(SupplyEscrowAuditRules.creationMatches(current, 10, 1_000L));
        assertFalse(SupplyEscrowAuditRules.creationMatches(current, 9, 1_000L));
        assertFalse(SupplyEscrowAuditRules.creationMatches(current, 10, 900L));
    }

    @Test
    void permitsLegacyQuantityWhenAmountStillMatches() {
        var legacy = new SupplyEscrowSavedData.Escrow("order-1", 0, 1_000L,
                1_000L, 0L, 0L);
        assertTrue(SupplyEscrowAuditRules.creationMatches(legacy, 10, 1_000L));
    }
}
