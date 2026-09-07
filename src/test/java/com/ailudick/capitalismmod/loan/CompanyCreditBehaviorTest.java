package com.ailudick.capitalismmod.loan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompanyCreditBehaviorTest {
    @Test
    void underwritingLeavesNewBusinessesUnpenalized() {
        assertEquals(1.0, CompanyCreditBehavior.underwritingMultiplier(0, 0), 0.0001);
    }

    @Test
    void underwritingRewardsGoodHistoryAndLimitsBadHistory() {
        assertEquals(1.0, CompanyCreditBehavior.underwritingMultiplier(10, 100), 0.0001);
        assertEquals(0.5, CompanyCreditBehavior.underwritingMultiplier(10, 0), 0.0001);
    }
}
