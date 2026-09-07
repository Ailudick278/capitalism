package com.ailudick.capitalismmod.population;

/** Pure household financial-stress calculation for policy and credit decisions. */
public final class HouseholdFinancialRisk {
    private HouseholdFinancialRisk() {}

    public static int score(long cashMinor, long dailyNeedMinor, long rentArrearsMinor,
                            long wageArrearsMinor, int unemploymentDays) {
        return score(cashMinor, dailyNeedMinor, rentArrearsMinor, wageArrearsMinor, 0L, unemploymentDays);
    }

    public static int score(long cashMinor, long dailyNeedMinor, long rentArrearsMinor,
                            long wageArrearsMinor, long bankDebtMinor, int unemploymentDays) {
        return score(cashMinor, dailyNeedMinor, rentArrearsMinor, wageArrearsMinor,
                bankDebtMinor, unemploymentDays, false);
    }

    public static int score(long cashMinor, long dailyNeedMinor, long rentArrearsMinor,
                            long wageArrearsMinor, long bankDebtMinor, int unemploymentDays,
                            boolean bankOverdue) {
        return score(cashMinor, dailyNeedMinor, rentArrearsMinor, wageArrearsMinor,
                bankDebtMinor, unemploymentDays, bankOverdue, 0);
    }

    /**
     * Adds a bounded repayment-history benefit. Recent valid repayments reduce
     * stress, but never erase current arrears or an overdue loan entirely.
     */
    public static int score(long cashMinor, long dailyNeedMinor, long rentArrearsMinor,
                            long wageArrearsMinor, long bankDebtMinor, int unemploymentDays,
                            boolean bankOverdue, int recentRepaymentCount) {
        if (cashMinor < 0L || dailyNeedMinor < 0L || rentArrearsMinor < 0L
                || wageArrearsMinor < 0L || bankDebtMinor < 0L || unemploymentDays < 0
                || recentRepaymentCount < 0) return 100;
        long shortfall = dailyNeedMinor > cashMinor ? dailyNeedMinor - cashMinor : 0L;
        int liquidity = dailyNeedMinor <= 0L ? 0
                : (int) Math.min(40L, shortfall * 40L / dailyNeedMinor);
        long debt = add(add(rentArrearsMinor, wageArrearsMinor), bankDebtMinor);
        long monthlyNeed = dailyNeedMinor > Long.MAX_VALUE / 30L
                ? Long.MAX_VALUE : dailyNeedMinor * 30L;
        int debtRisk = monthlyNeed <= 0L ? (debt > 0L ? 30 : 0)
                : (int) Math.min(30L, debt * 30L / monthlyNeed);
        int unemploymentRisk = Math.min(20, unemploymentDays * 20 / 90);
        int overdueRisk = bankOverdue ? 15 : 0;
        int repaymentRelief = Math.min(10, recentRepaymentCount * 2);
        return Math.max(0, Math.min(100, liquidity + debtRisk + unemploymentRisk + overdueRisk - repaymentRelief));
    }

    /** Annual interest-rate premium: 0% at no stress, up to 8% at maximum stress. */
    public static double interestPremium(int financialRisk) {
        return Math.max(0, Math.min(100, financialRisk)) * 0.0008;
    }

    private static long add(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }
}
