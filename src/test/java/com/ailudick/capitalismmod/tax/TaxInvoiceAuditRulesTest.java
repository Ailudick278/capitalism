package com.ailudick.capitalismmod.tax;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxInvoiceAuditRulesTest {
    @Test
    void acceptsValidOutputInvoice() {
        assertTrue(TaxInvoiceAuditRules.valid("i-1", "freight:s-1", UUID.randomUUID(), "usd",
                10000L, 1300L, 200L, 10L, "output", true));
    }

    @Test
    void rejectsInvalidDirectionAndCreditOverflow() {
        assertFalse(TaxInvoiceAuditRules.valid("i-1", "s-1", UUID.randomUUID(), "usd",
                10000L, 100L, 101L, 10L, "output", true));
        assertFalse(TaxInvoiceAuditRules.valid("i-1", "s-1", UUID.randomUUID(), "usd",
                10000L, 100L, 0L, 10L, "refund", true));
    }
}
