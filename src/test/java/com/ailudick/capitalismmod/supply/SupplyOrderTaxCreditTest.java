package com.ailudick.capitalismmod.supply;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SupplyOrderTaxCreditTest {
    @Test
    void proportionalInputCreditDoesNotOverflowForLargeOrders() {
        assertEquals(Long.MAX_VALUE / 2,
                SupplyOrderTaxCreditCalculator.proportional(Long.MAX_VALUE, 1, 2));
    }

    @Test
    void proportionalInputCreditRoundsDownAndNeverExceedsTheOriginalCredit() {
        assertEquals(6L, SupplyOrderTaxCreditCalculator.proportional(10L, 2, 3));
    }
}
