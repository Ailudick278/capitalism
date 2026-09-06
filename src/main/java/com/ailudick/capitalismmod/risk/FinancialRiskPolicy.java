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

    /** Bank credit is tightened once delinquency becomes material, and frozen at severe stress. */
    public static boolean newBankCreditAllowed(int overdueShareBasisPoints) {
        return overdueShareBasisPoints < 6000;
    }

    /** Below 20% delinquency share, ordinary bank underwriting remains available. */
    public static boolean bankCapacityRestrictionActive(int overdueShareBasisPoints) {
        return overdueShareBasisPoints >= 2000;
    }

    public static boolean crisisTriggered(int overdueShareBasisPoints) {
        return overdueShareBasisPoints >= 6000;
    }

    public static boolean crisisRecovered(int overdueShareBasisPoints) {
        return overdueShareBasisPoints <= 4000;
    }

    /** Adds a bounded liquidity premium to newly issued government bonds. */
    public static double bondLiquidityPremium(int overdueShareBasisPoints) {
        int share = Math.max(0, Math.min(10000, overdueShareBasisPoints));
        return Math.min(0.05, Math.max(0, share - 2000) / 10000.0 * 0.05);
    }
}
