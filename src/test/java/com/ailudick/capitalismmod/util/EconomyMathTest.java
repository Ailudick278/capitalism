package com.ailudick.capitalismmod.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EconomyMathTest {
    @Test
    void reportsOverflowWithSentinel() {
        assertEquals(-1L, EconomyMath.multiply(Long.MAX_VALUE, 2));
        assertEquals(-1L, EconomyMath.add(Long.MAX_VALUE, 1));
    }

    @Test
    void preservesSignedShareDeltas() {
        assertEquals(-5L, EconomyMath.add(10, -15));
    }
}
