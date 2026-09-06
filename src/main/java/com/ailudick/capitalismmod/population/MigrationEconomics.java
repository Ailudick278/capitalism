package com.ailudick.capitalismmod.population;

/** Pure regional migration arithmetic; deliberately independent of Minecraft state. */
public final class MigrationEconomics {
    private MigrationEconomics() {}

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
}
