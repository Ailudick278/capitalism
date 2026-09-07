package com.ailudick.capitalismmod.bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CentralBankFacilityEconomicsTest {
    @Test
    void validatesTermsAndCalculatesBoundedInstallments() {
        assertTrue(CentralBankFacilityEconomics.validTerms(100_000L, 500, 30));
        assertFalse(CentralBankFacilityEconomics.validTerms(0L, 500, 30));
        assertEquals(34L, CentralBankFacilityEconomics.principalInstallment(100L, 3));
        assertEquals(411L, CentralBankFacilityEconomics.interestDue(100_000L, 500, 30));
    }
}
