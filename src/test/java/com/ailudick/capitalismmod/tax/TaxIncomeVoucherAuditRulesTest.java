package com.ailudick.capitalismmod.tax;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxIncomeVoucherAuditRulesTest {
    @Test
    void validatesVoucherFields() {
        assertTrue(TaxIncomeVoucherAuditRules.valid(UUID.randomUUID(), "company-1", "corporate_income",
                "usd", 100L, 10L, "freight:s-1", true));
        assertFalse(TaxIncomeVoucherAuditRules.valid(UUID.randomUUID(), "company-1", "corporate_income",
                "unknown", 100L, 10L, "freight:s-1", false));
        assertFalse(TaxIncomeVoucherAuditRules.valid(UUID.randomUUID(), "company-1", "corporate_income",
                "usd", 0L, 10L, "freight:s-1", true));
    }
}
