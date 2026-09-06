package com.ailudick.capitalismmod.government;

/** Pure cost and schedule rules for public construction projects. */
public final class PublicConstructionEconomics {
    private PublicConstructionEconomics() {}

    public static long unitCost(String facility) {
        return switch (facility) {
            case "housing" -> 50_000L;
            case "school" -> 300_000L;
            case "clinic" -> 500_000L;
            default -> 0L;
        };
    }

    public static boolean validFacility(String facility) {
        return unitCost(facility) > 0L;
    }
}
