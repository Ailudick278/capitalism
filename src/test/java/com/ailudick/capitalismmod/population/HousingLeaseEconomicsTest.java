package com.ailudick.capitalismmod.population;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HousingLeaseEconomicsTest {
    @Test
    void securityDepositIsThirtyDaysOfRent() {
        assertEquals(3_000L, HousingLeaseEconomics.securityDeposit(100L));
    }

    @Test
    void paymentsClearOldArrearsBeforeCurrentRent() {
        HousingLeaseEconomics.Settlement settlement = HousingLeaseEconomics.settle(
                300L, 4, 6L, 10L, 100L, 250L);
        assertEquals(400L, settlement.totalDue());
        assertEquals(250L, settlement.paid());
        assertEquals(150L, settlement.arrears());
        assertEquals(5, settlement.missedDays());
        assertEquals("grace", settlement.status());
    }

    @Test
    void noticeAndTerminationThresholdsAreStable() {
        assertEquals("notice", HousingLeaseEconomics.settle(1L, 29, -1L, 30L, 1L, 0L).status());
        assertEquals("termination_eligible", HousingLeaseEconomics.settle(1L, 89, 30L, 90L, 1L, 0L).status());
        assertEquals(30L, HousingLeaseEconomics.settle(1L, 29, -1L, 30L, 1L, 0L).noticeDay());
    }

    @Test
    void depositFirstOffsetsArrearsThenReturnsRemainder() {
        HousingLeaseEconomics.Termination result = HousingLeaseEconomics.terminate(500L, 300L);
        assertEquals(200L, result.refund());
        assertEquals(0L, result.residualArrears());
        assertEquals(0L, HousingLeaseEconomics.terminate(500L, 700L).refund());
        assertEquals(200L, HousingLeaseEconomics.terminate(500L, 700L).residualArrears());
    }
}
