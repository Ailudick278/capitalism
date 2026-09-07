package com.ailudick.capitalismmod.population;

/** Pure regional migration arithmetic; deliberately independent of Minecraft state. */
public final class MigrationEconomics {
    private MigrationEconomics() {}

    /**
     * Service quality can lower, but never eliminate, the income required to move.
     * A 30-point service advantage provides the maximum 20% relief.
     */
    public static boolean willingToMove(int satisfaction, int originServices, int destinationServices,
                                        long dailyWageMinor, long requiredDailyIncome) {
        return willingToMove(satisfaction, originServices, destinationServices,
                dailyWageMinor, requiredDailyIncome, 0);
    }

    public static boolean willingToMove(int satisfaction, int originServices, int destinationServices,
                                        long dailyWageMinor, long requiredDailyIncome, int financialRisk) {
        int risk = Math.max(0, Math.min(100, financialRisk));
        if (dailyWageMinor < 0L || requiredDailyIncome < 0L
                || satisfaction - risk / 4 > 40) return false;
        int advantage = Math.max(0, Math.min(30, destinationServices - originServices));
        long relief = requiredDailyIncome * advantage / 150L;
        long threshold = Math.max(0L, requiredDailyIncome - relief);
        return dailyWageMinor >= threshold;
    }

    public static long friction(long dailyNeedMinor, int householdSize, int accessScore) {
        if (dailyNeedMinor <= 0L || householdSize <= 0 || accessScore >= 100) return 0L;
        long base;
        try {
            base = Math.multiplyExact(dailyNeedMinor, householdSize);
        } catch (ArithmeticException e) {
            base = Long.MAX_VALUE;
        }
        long rate = 100L - Math.max(0, Math.min(100, accessScore));
        return base > Long.MAX_VALUE / rate ? Long.MAX_VALUE : base * rate / 100L;
    }

    /** Adjusts household living cost for a regional essential-goods price premium. */
    public static long costOfLiving(long baseDailyCostMinor, int pricePremiumBps) {
        if (baseDailyCostMinor <= 0L) return 0L;
        long premium = Math.max(0L, Math.min(10_000L, pricePremiumBps));
        if (baseDailyCostMinor > (Long.MAX_VALUE - premium) / 10_000L) return Long.MAX_VALUE;
        return baseDailyCostMinor + baseDailyCostMinor * premium / 10_000L;
    }
}
