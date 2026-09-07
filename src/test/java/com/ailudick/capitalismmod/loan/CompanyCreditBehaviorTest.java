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
        assertEquals(0.0, CompanyCreditBehavior.riskPremiumRate(0, 0), 0.0001);
        assertEquals(0.05, CompanyCreditBehavior.riskPremiumRate(10, 0), 0.0001);
        assertEquals(1.0, CompanyCreditBehavior.termMultiplier(0, 0), 0.0001);
        assertEquals(0.5, CompanyCreditBehavior.termMultiplier(10, 0), 0.0001);
        assertEquals(1.0, CompanyCreditBehavior.termMultiplier(10, 100), 0.0001);
        assertEquals(90, CompanyCreditBehavior.scoreForRepaymentAmounts(900, 100));
        assertEquals(0, CompanyCreditBehavior.scoreForRepaymentAmounts(0, 500));
        assertEquals(0L, CompanyCreditBehavior.requiredCollateral(1_000L, 0, 0));
        assertEquals(500L, CompanyCreditBehavior.requiredCollateral(1_000L, 10, 100));
        assertEquals(1_500L, CompanyCreditBehavior.requiredCollateral(1_000L, 10, 0));
    }
}
