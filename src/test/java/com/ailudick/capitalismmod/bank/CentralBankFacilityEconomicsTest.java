package com.ailudick.capitalismmod.bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;

class CentralBankFacilityEconomicsTest {
    @Test
    void validatesTermsAndCalculatesBoundedInstallments() {
        assertTrue(CentralBankFacilityEconomics.validTerms(100_000L, 500, 30));
        assertFalse(CentralBankFacilityEconomics.validTerms(0L, 500, 30));
        assertEquals(34L, CentralBankFacilityEconomics.principalInstallment(100L, 3));
        assertEquals(411L, CentralBankFacilityEconomics.interestDue(100_000L, 500, 30));
        assertTrue(CentralBankFacilityAuditRules.validHistory(List.of(
                new CentralBankFacilitySavedData.Facility("f", 1L, 100L, 100L, 500, 30, 30, -1L))));
        assertFalse(CentralBankFacilityAuditRules.validHistory(List.of(
                new CentralBankFacilitySavedData.Facility("f", 1L, 100L, 101L, 500, 30, 30, -1L))));
    }
}
