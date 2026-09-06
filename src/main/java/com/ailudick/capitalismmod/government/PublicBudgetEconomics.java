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
}
