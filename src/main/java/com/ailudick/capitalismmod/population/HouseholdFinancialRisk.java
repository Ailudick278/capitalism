package com.ailudick.capitalismmod.population;

/** Pure household financial-stress calculation for policy and credit decisions. */
public final class HouseholdFinancialRisk {
    private HouseholdFinancialRisk() {}

    public static int score(long cashMinor, long dailyNeedMinor, long rentArrearsMinor,
                            long wageArrearsMinor, int unemploymentDays) {
        if (cashMinor < 0L || dailyNeedMinor < 0L || rentArrearsMinor < 0L
                || wageArrearsMinor < 0L || unemploymentDays < 0) return 100;
        long shortfall = dailyNeedMinor > cashMinor ? dailyNeedMinor - cashMinor : 0L;
        int liquidity = dailyNeedMinor <= 0L ? 0
                : (int) Math.min(40L, shortfall * 40L / dailyNeedMinor);
        long debt = add(rentArrearsMinor, wageArrearsMinor);
        long monthlyNeed = dailyNeedMinor > Long.MAX_VALUE / 30L
                ? Long.MAX_VALUE : dailyNeedMinor * 30L;
        int debtRisk = monthlyNeed <= 0L ? (debt > 0L ? 30 : 0)
                : (int) Math.min(30L, debt * 30L / monthlyNeed);
        int unemploymentRisk = Math.min(20, unemploymentDays * 20 / 90);
        return Math.min(100, liquidity + debtRisk + unemploymentRisk);
    }

    private static long add(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }
}
