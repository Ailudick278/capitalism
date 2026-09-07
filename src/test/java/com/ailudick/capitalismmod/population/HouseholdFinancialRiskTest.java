package com.ailudick.capitalismmod.population;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HouseholdFinancialRiskTest {
    @Test
    void liquidityDebtAndUnemploymentIncreaseRisk() {
        assertEquals(0, HouseholdFinancialRisk.score(3_000L, 3_000L, 0L, 0L, 0));
        int stressed = HouseholdFinancialRisk.score(0L, 3_000L, 90_000L, 0L, 90);
        assertEquals(90, stressed);
    }

    @Test
    void invalidInputsAreHighRisk() {
        assertEquals(100, HouseholdFinancialRisk.score(-1L, 1L, 0L, 0L, 0));
    }

    @Test
    void interestPremiumScalesWithRisk() {
        assertEquals(0.0, HouseholdFinancialRisk.interestPremium(0));
        assertEquals(0.04, HouseholdFinancialRisk.interestPremium(50), 0.000001);
        assertEquals(0.08, HouseholdFinancialRisk.interestPremium(100), 0.000001);
    }

    @Test
    void bankDebtAddsToDebtRisk() {
        assertEquals(0, HouseholdFinancialRisk.score(3_000L, 3_000L, 0L, 0L, 0L, 0));
        assertEquals(30, HouseholdFinancialRisk.score(3_000L, 3_000L, 0L, 0L, 90_000L, 0));
        assertEquals(15, HouseholdFinancialRisk.score(3_000L, 3_000L, 0L, 0L, 0L, 0, true));
    }
}
