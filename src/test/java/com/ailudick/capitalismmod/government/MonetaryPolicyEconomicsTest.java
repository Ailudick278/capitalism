package com.ailudick.capitalismmod.government;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonetaryPolicyEconomicsTest {
    @Test
    void policyRateShiftsConfiguredAnnualRateByBasisPoints() {
        assertEquals(0.07, MonetaryPolicyEconomics.adjustedAnnualRate(0.05, 200), 0.000001);
        assertEquals(0.03, MonetaryPolicyEconomics.adjustedAnnualRate(0.05, -200), 0.000001);
        assertEquals(0.0, MonetaryPolicyEconomics.adjustedAnnualRate(0.01, -10000), 0.000001);
    }
}
