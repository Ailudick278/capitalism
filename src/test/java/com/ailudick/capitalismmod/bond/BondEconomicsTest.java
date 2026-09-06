package com.ailudick.capitalismmod.bond;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BondEconomicsTest {
    @Test
    void couponUsesWholeUnitDownwardRounding() {
        assertEquals(100L, BondEconomics.maturityCoupon(1000L, 0.10, 365));
        assertEquals(27L, BondEconomics.maturityCoupon(1000L, 0.10, 100));
    }

    @Test
    void invalidInputsDoNotCreateCash() {
        assertEquals(0L, BondEconomics.maturityCoupon(0L, 0.10, 365));
        assertEquals(0L, BondEconomics.maturityCoupon(1000L, -0.01, 365));
        assertEquals(0L, BondEconomics.maturityCoupon(1000L, Double.NaN, 365));
        assertEquals(0L, BondEconomics.maturityCoupon(1000L, 0.10, 0));
    }

    @Test
    void overflowingCalculationSaturates() {
        assertEquals(Long.MAX_VALUE,
                BondEconomics.maturityCoupon(Long.MAX_VALUE, Double.MAX_VALUE, Integer.MAX_VALUE));
    }
}
