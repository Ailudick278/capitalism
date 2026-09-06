package com.ailudick.capitalismmod.population;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HousingLeaseEconomicsTest {
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
}
