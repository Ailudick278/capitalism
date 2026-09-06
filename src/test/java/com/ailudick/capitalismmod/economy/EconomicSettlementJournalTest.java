package com.ailudick.capitalismmod.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EconomicSettlementJournalTest {
    @Test
    void phaseKeyIsStableAcrossRetries() {
        assertEquals("42:credit-and-securities",
                EconomicSettlementJournalKey.of(42L, " credit-and-securities "));
        assertEquals("42:", EconomicSettlementJournalKey.of(42L, null));
    }
}
