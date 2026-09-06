package com.ailudick.capitalismmod.government;

/** Deterministic daily public-facility maintenance costs in the base currency. */
public final class PublicBudgetEconomics {
    private PublicBudgetEconomics() {}

    public static long dailyMaintenance(String facility, int count) {
        if (facility == null || count <= 0) return 0L;
        long perUnit = switch (facility) {
            case "housing" -> 50L;
            case "school" -> 300L;
            case "clinic" -> 500L;
            default -> 0L;
        };
        if (perUnit == 0L) return 0L;
        if (count > Long.MAX_VALUE / perUnit) return Long.MAX_VALUE;
        return perUnit * count;
    }

    /** Higher priority services are maintained first during fiscal stress. */
    public static int priority(String facility) {
        return switch (facility) {
            case "housing" -> 100;
            case "clinic" -> 90;
            case "school" -> 80;
            default -> 0;
        };
    }

    /** 0 means at least a week of projected costs is covered; 100 means none. */
    public static int fiscalStress(long treasury, long dailyCosts) {
        if (dailyCosts <= 0L) return 0;
        long weeklyCosts = dailyCosts > Long.MAX_VALUE / 7L ? Long.MAX_VALUE : dailyCosts * 7L;
        if (treasury <= 0L) return 100;
        if (treasury >= weeklyCosts) return 0;
        long coveredPercent = (long) Math.min(100D, (double) treasury * 100D / (double) weeklyCosts);
        return (int) Math.max(0L, Math.min(100L, 100L - coveredPercent));
    }
}
