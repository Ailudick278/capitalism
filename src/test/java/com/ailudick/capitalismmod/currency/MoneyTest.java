package com.ailudick.capitalismmod.currency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTest {
    @Test
    void convertsMajorUnitsWithoutLoss() {
        assertEquals(1250L, Money.toMinor(12));
        assertEquals(-1L, Money.toMinor(Long.MAX_VALUE));
    }

    @Test
    void formatsMinorUnits() {
        assertEquals("12.50", Money.format(1250));
        assertEquals("0.05", Money.format(5));
    }
}
