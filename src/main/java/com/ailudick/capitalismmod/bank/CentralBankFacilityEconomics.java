package com.ailudick.capitalismmod.bank;

/** Pure rules for a time-limited central-bank liquidity facility. */
public final class CentralBankFacilityEconomics {
    private CentralBankFacilityEconomics() {}

    public static long interestDue(long principalMinor, int annualRateBasisPoints, int days) {
        if (principalMinor <= 0L || days <= 0 || annualRateBasisPoints <= 0) return 0L;
        double value = (double) principalMinor * annualRateBasisPoints * days / (365.0 * 10_000.0);
        return !Double.isFinite(value) || value >= Long.MAX_VALUE ? Long.MAX_VALUE
                : Math.max(0L, (long) Math.ceil(value));
    }

    /** Equal-principal installment, with the final installment absorbing rounding. */
    public static long principalInstallment(long remainingPrincipal, int daysRemaining) {
        if (remainingPrincipal <= 0L || daysRemaining <= 0) return 0L;
        return remainingPrincipal / daysRemaining
                + (remainingPrincipal % daysRemaining == 0L ? 0L : 1L);
    }

    public static boolean validTerms(long principalMinor, int annualRateBasisPoints,
                                     int termDays) {
        return principalMinor > 0L && annualRateBasisPoints >= 0 && annualRateBasisPoints <= 100_000
                && termDays > 0 && termDays <= 360;
    }
}
