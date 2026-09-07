package com.ailudick.capitalismmod.company;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyLedgerSourceRulesTest {
    @Test
    void sourceReceiptMustMatchCurrencyAmountAndDirection() {
        CompanyLedgerEntry credit = new CompanyLedgerEntry("c", 1L, "revenue", "usd", 100L, 100L, "[source=s]");
        CompanyLedgerEntry debit = new CompanyLedgerEntry("c", 1L, "expense", "usd", -100L, 0L, "[source=s]");
        assertTrue(CompanyLedgerSourceRules.matches(credit, "usd", 100L, true));
        assertFalse(CompanyLedgerSourceRules.matches(credit, "usd", 90L, true));
        assertFalse(CompanyLedgerSourceRules.matches(credit, "eur", 100L, true));
        assertTrue(CompanyLedgerSourceRules.matches(debit, "usd", 100L, false));
    }
}
