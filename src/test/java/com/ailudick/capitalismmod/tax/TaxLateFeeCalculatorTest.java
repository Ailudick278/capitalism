package com.ailudick.capitalismmod.tax;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaxLateFeeCalculatorTest {
    @Test
    void usesFiveBasisPointsByDefault() {
        assertEquals(5L, TaxLateFeeCalculator.calculate(10_000L, 1L, 0.0005));
        assertEquals(15L, TaxLateFeeCalculator.calculate(10_000L, 3L, 0.0005));
    }

    @Test
    void chargesAtLeastOneMinorUnitPerLateDay() {
        assertEquals(4L, TaxLateFeeCalculator.calculate(1L, 4L, 0.0005));
    }

    @Test
    void rejectsInvalidInputsAndProtectsOverflow() {
        assertEquals(0L, TaxLateFeeCalculator.calculate(0L, 4L, 0.0005));
        assertEquals(0L, TaxLateFeeCalculator.calculate(10L, 0L, 0.0005));
        assertEquals(0L, TaxLateFeeCalculator.calculate(10L, 4L, Double.NaN));
        assertEquals(Long.MAX_VALUE, TaxLateFeeCalculator.calculate(Long.MAX_VALUE, Long.MAX_VALUE, 1.0));
    }
}
