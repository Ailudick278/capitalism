package com.ailudick.capitalismmod.bond;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure bond cash-flow calculations using deterministic monetary rounding. */
public final class BondEconomics {
    private BondEconomics() {}

    /** Returns the coupon in major currency units, rounded down to a whole unit. */
    public static long maturityCoupon(long faceValue, double annualRate, int totalDays) {
        if (faceValue <= 0L || !Double.isFinite(annualRate) || annualRate < 0.0 || totalDays <= 0) {
            return 0L;
        }
        try {
            return BigDecimal.valueOf(faceValue)
                    .multiply(BigDecimal.valueOf(annualRate))
                    .multiply(BigDecimal.valueOf(totalDays))
                    .divide(BigDecimal.valueOf(365L), 0, RoundingMode.DOWN)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }
}
