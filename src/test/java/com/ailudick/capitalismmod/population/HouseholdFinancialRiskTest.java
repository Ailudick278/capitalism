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
}
