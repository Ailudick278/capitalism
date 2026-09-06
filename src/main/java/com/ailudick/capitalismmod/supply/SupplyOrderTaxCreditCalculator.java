package com.ailudick.capitalismmod.supply;

import java.math.BigInteger;

/** Pure arithmetic for reversing input VAT on an undelivered order portion. */
public final class SupplyOrderTaxCreditCalculator {
    private SupplyOrderTaxCreditCalculator() {
    }

    public static long proportional(long inputCreditMinor, int remaining, int originalQuantity) {
        if (inputCreditMinor <= 0L || remaining <= 0 || originalQuantity <= 0) return 0L;
        return BigInteger.valueOf(inputCreditMinor)
                .multiply(BigInteger.valueOf(remaining))
                .divide(BigInteger.valueOf(originalQuantity))
                .min(BigInteger.valueOf(inputCreditMinor))
                .longValue();
    }
}
