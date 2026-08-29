package com.ailudick.capitalismmod.util;

/**
 * Overflow-safe arithmetic for economy amounts.
 * Amounts, quantities, and prices are always non-negative, so {@code -1} is a
 * safe sentinel returned when a result would overflow a {@code long}.
 */
public final class EconomyMath {
    private EconomyMath() {
    }

    /** Returns {@code a * b}, or {@code -1} on overflow. */
    public static long multiply(long a, long b) {
        try {
            return Math.multiplyExact(a, b);
        } catch (ArithmeticException e) {
            return -1;
        }
    }

    /** Returns {@code a + b}, or {@code -1} on overflow. */
    public static long add(long a, long b) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException e) {
            return -1;
        }
    }
}
