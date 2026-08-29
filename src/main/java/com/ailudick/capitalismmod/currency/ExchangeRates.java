package com.ailudick.capitalismmod.currency;

import java.math.BigInteger;

/**
 * Exchange-rate math between currencies, based on each currency's {@code baseValue}.
 * Amounts are in minor units. Uses {@link BigInteger} so the intermediate
 * {@code amount * baseValue} product cannot overflow a {@code long}.
 */
public final class ExchangeRates {
    private ExchangeRates() {
    }

    /** Converts {@code amount} minor units of {@code from} to minor units of {@code to}, rounding to nearest. */
    public static long convert(long amount, Currency from, Currency to) {
        BigInteger a = BigInteger.valueOf(amount)
                .multiply(BigInteger.valueOf(ExchangeRateProvider.effective(from)));
        BigInteger b = BigInteger.valueOf(ExchangeRateProvider.effective(to));
        BigInteger result = a.add(b.shiftRight(1)).divide(b);
        if (result.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
            return Long.MAX_VALUE;
        }
        return result.longValue();
    }
}
