package com.ailudick.capitalismmod.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure calculation for daily tax late fees, kept independent from saved data and servers. */
public final class TaxLateFeeCalculator {
    private TaxLateFeeCalculator() {}

    public static long calculate(long taxAmount, long daysLate, double ratePerDay) {
        if (taxAmount <= 0L || daysLate <= 0L || !Double.isFinite(ratePerDay) || ratePerDay <= 0.0) {
            return 0L;
        }
        long daily;
        try {
            daily = BigDecimal.valueOf(taxAmount)
                    .multiply(BigDecimal.valueOf(ratePerDay))
                    .setScale(0, RoundingMode.DOWN).longValueExact();
        } catch (ArithmeticException e) {
            daily = Long.MAX_VALUE;
        }
        daily = Math.max(1L, daily);
        if (daily > Long.MAX_VALUE / daysLate) return Long.MAX_VALUE;
        return daily * daysLate;
    }
}
