package com.ailudick.capitalismmod.government;

import java.util.Map;
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

    /** Material bill per facility unit, expressed as warehouse item ids. */
    public static Map<String, Integer> materials(String facility) {
        return switch (facility) {
            case "housing" -> Map.of("minecraft:stone", 16, "minecraft:oak_planks", 8);
            case "school" -> Map.of("minecraft:stone", 48, "minecraft:oak_planks", 16, "minecraft:glass", 16);
            case "clinic" -> Map.of("minecraft:stone", 64, "minecraft:glass", 24, "minecraft:iron_ingot", 8);
            default -> Map.of();
        };
    }
}
