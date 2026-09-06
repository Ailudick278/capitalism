package com.ailudick.capitalismmod.risk;

/** Rules translating sector-wide delinquency into proportional credit tightening. */
public final class FinancialRiskPolicy {
    private FinancialRiskPolicy() {}

    /** 0-10000 basis-point delinquency share to a 0.25-1.00 credit multiplier. */
    public static double creditMultiplier(int overdueShareBasisPoints) {
        int share = Math.max(0, Math.min(10000, overdueShareBasisPoints));
        return Math.max(0.25, 1.0 - share / 10000.0);
    }

    /** New company credit is frozen only in a severe, system-wide delinquency state. */
    public static boolean newCompanyCreditAllowed(int overdueShareBasisPoints) {
        return overdueShareBasisPoints < 6000;
    }
}
