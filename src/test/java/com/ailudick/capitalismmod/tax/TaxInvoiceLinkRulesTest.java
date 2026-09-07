package com.ailudick.capitalismmod.tax;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxInvoiceLinkRulesTest {
    @Test
    void fullyCreditedInvoiceNeedsNoOutputBill() {
        assertTrue(TaxInvoiceLinkRules.matchesBill("output", 10000L, 1300L, 1300L,
                UUID.randomUUID(), "usd", "vat:s-1", null));
        assertTrue(TaxInvoiceLinkRules.matchesBill("input", 10000L, 1300L, 1300L,
                UUID.randomUUID(), "usd", "vat-input:s-1", null));
    }

    @Test
    void outputInvoiceMustMatchItsBill() {
        UUID taxpayer = UUID.randomUUID();
        TaxBill bill = new TaxBill("bill-1", new TaxSubject(TaxType.VAT, "vat:" + taxpayer, taxpayer),
                "usd", 1100L, 0L, 10L, 20L, 30L, "vat:s-1", 0L, 100L,
                10000L, 1300);
        assertTrue(TaxInvoiceLinkRules.matchesBill("output", 10000L, 1300L, 200L,
                taxpayer, "usd", "vat:s-1", bill));
        assertFalse(TaxInvoiceLinkRules.matchesBill("output", 10000L, 1300L, 100L,
                taxpayer, "usd", "vat:s-1", bill));
    }
}
