package com.ailudick.capitalismmod.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementJournalRulesTest {
    @Test
    void acceptsStartedAndCompletedFinancialEntries() {
        assertTrue(SettlementJournalRules.validFinancial("trade-1", "stock", "shares",
                "started", 100L, 20L));
        assertTrue(SettlementJournalRules.validFinancial("trade-1", "stock", "shares",
                "completed", 100L, 21L));
    }

    @Test
    void rejectsMalformedFinancialAndDailyEntries() {
        assertFalse(SettlementJournalRules.validFinancial("", "stock", "shares", "started", 1L, 1L));
        assertFalse(SettlementJournalRules.validFinancial("trade-1", "stock", "shares", "failed", 1L, 1L));
        assertFalse(SettlementJournalRules.validFinancial("trade-1", "stock", "shares", "started", -1L, 1L));
        assertFalse(SettlementJournalRules.validDaily(-1L, "markets-and-close", "started", 1L));
        assertFalse(SettlementJournalRules.validDaily(1L, "markets-and-close", "started", -1L));
    }
}
