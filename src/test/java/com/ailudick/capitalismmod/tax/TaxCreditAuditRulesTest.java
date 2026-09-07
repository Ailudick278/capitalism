package com.ailudick.capitalismmod.tax;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxCreditAuditRulesTest {
    @Test
    void validatesCreditLot() {
        assertTrue(TaxCreditAuditRules.validLot(UUID.randomUUID(), "usd", "vat", "subject",
                "vat-input:source", 0L, 100L, 10L, 50L, true));
        assertFalse(TaxCreditAuditRules.validLot(UUID.randomUUID(), "usd", "vat", "subject",
                "vat-input:source", 100L, 0L, 10L, 50L, true));
    }

    @Test
    void aggregateCannotBeBelowVisibleLots() {
        assertTrue(TaxCreditAuditRules.aggregateCovers(100L, 75L));
        assertFalse(TaxCreditAuditRules.aggregateCovers(50L, 75L));
    }
}
