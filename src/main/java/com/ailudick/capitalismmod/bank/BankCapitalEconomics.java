package com.ailudick.capitalismmod.bank;

/** Pure capital-adequacy arithmetic for the banking model. */
public final class BankCapitalEconomics {
    private BankCapitalEconomics() {}

    /** Returns whether equity is below the 8% minimum against loan exposure. */
    public static boolean capitalStress(long capitalMinor, long loansMinor) {
        if (loansMinor <= 0L) return capitalMinor < 0L;
        if (capitalMinor <= 0L) return true;
        long required = loansMinor / 100L * 8L + (loansMinor % 100L) * 8L / 100L;
        return capitalMinor < required;
    }

    /** Maximum loan exposure supported by an 8% capital adequacy floor. */
    public static long capitalBackedLoanCapacity(long capitalMinor) {
        if (capitalMinor <= 0L) return 0L;
        long whole = capitalMinor / 8L;
        long remainder = capitalMinor % 8L;
        if (whole > Long.MAX_VALUE / 100L) return Long.MAX_VALUE;
        return whole * 100L + remainder * 100L / 8L;
    }
}
