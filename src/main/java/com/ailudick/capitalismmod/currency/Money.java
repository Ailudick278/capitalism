package com.ailudick.capitalismmod.currency;

/**
 * Money units: internally all money is stored in <b>minor units</b> (the smallest
 * coin denomination — cents, fen, kopecks), where {@value #MINOR_UNITS_PER_UNIT}
 * minor units make one major unit. Prices in the economy (shop, stock, company)
 * stay in major units and are converted at payment boundaries via {@link #toMinor}.
 */
public final class Money {
    public static final int MINOR_UNITS_PER_UNIT = 100;

    private Money() {
    }

    /** Converts a major-unit amount (whole dollars / yuan / ...) to minor units. */
    public static long toMinor(long major) {
        return major * MINOR_UNITS_PER_UNIT;
    }

    /** Formats a minor-unit amount as a major-unit string ("7", "7.25", "-0.05"). */
    public static String format(long minor) {
        long major = minor / MINOR_UNITS_PER_UNIT;
        long rem = Math.abs(minor % MINOR_UNITS_PER_UNIT);
        if (rem == 0) {
            return Long.toString(major);
        }
        return major + "." + String.format("%02d", rem);
    }
}
