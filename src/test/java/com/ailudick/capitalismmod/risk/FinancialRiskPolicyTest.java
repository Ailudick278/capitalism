package com.ailudick.capitalismmod.risk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialRiskPolicyTest {
    @Test
    void tightensCreditProportionallyAndFreezesOnlySevereCrisis() {
        assertEquals(1.0, FinancialRiskPolicy.creditMultiplier(0), 0.000001);
        assertEquals(0.8, FinancialRiskPolicy.creditMultiplier(2000), 0.000001);
        assertEquals(0.25, FinancialRiskPolicy.creditMultiplier(10000), 0.000001);
        assertTrue(FinancialRiskPolicy.newCompanyCreditAllowed(5999));
        assertFalse(FinancialRiskPolicy.newCompanyCreditAllowed(6000));
        assertTrue(FinancialRiskPolicy.crisisTriggered(6000));
        assertFalse(FinancialRiskPolicy.crisisRecovered(4001));
        assertTrue(FinancialRiskPolicy.crisisRecovered(4000));
    }
}
