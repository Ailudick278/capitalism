package com.ailudick.capitalismmod.bank;

/** Pure capital-adequacy arithmetic for the banking model. */
public final class BankCapitalEconomics {
    private BankCapitalEconomics() {}

    /** Returns whether equity is below the 8% minimum against loan exposure. */
    public static boolean capitalStress(long capitalMinor, long loansMinor) {
        if (loansMinor <= 0L) return capitalMinor < 0L;
        if (capitalMinor <= 0L) return true;
        long required = requiredCapital(loansMinor);
        return capitalMinor < required;
    }

    /** Minimum equity required for an 8% capital ratio, with overflow-safe arithmetic. */
    public static long requiredCapital(long loansMinor) {
        if (loansMinor <= 0L) return 0L;
        long whole = loansMinor / 100L;
        long remainder = loansMinor % 100L;
        if (whole > Long.MAX_VALUE / 8L) return Long.MAX_VALUE;
        long result = whole * 8L + remainder * 8L / 100L;
        return result < 0L ? Long.MAX_VALUE : result;
    }

    /** Positive fiscal support needed to restore the minimum capital ratio. */
    public static long capitalGap(long capitalMinor, long loansMinor) {
        long required = requiredCapital(loansMinor);
        long capital = Math.max(0L, capitalMinor);
        return required > capital ? required - capital : 0L;
    }

    /** Maximum loan exposure supported by an 8% capital adequacy floor. */
    public static long capitalBackedLoanCapacity(long capitalMinor) {
        if (capitalMinor <= 0L) return 0L;
        long whole = capitalMinor / 8L;
        long remainder = capitalMinor % 8L;
        if (whole > Long.MAX_VALUE / 100L) return Long.MAX_VALUE;
        return whole * 100L + remainder * 100L / 8L;
    }

    /** Conservative 50% loss provision against overdue loan exposure. */
    public static long lossProvisionTarget(long overdueDebtMinor) {
        if (overdueDebtMinor <= 0L) return 0L;
        return overdueDebtMinor / 2L + overdueDebtMinor % 2L;
    }

    /** Positive magnitude that remains representable for signed 64-bit values. */
    public static long positiveMagnitude(long value) {
        return value == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(value);
    }
}
