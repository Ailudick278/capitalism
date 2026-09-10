package com.ailudick.capitalismmod.company;

import java.util.Map;

/**
 * Physical energy inputs used by production recipes.
 *
 * <p>These are inventory commodities, separate from {@code energyCost},
 * which is the cash operating cost of a production cycle. Keeping the two
 * concepts separate makes fuel shortages visible without pretending that a
 * cash payment creates fuel.</p>
 */
public final class ProductionEnergyEconomics {
    private ProductionEnergyEconomics() {
    }

    public static boolean isEnergyItem(String itemId) {
        return switch (itemId == null ? "" : itemId) {
            case "minecraft:coal", "minecraft:charcoal",
                    "capitalismmod:fuel_oil", "capitalismmod:diesel",
                    "capitalismmod:gasoline", "capitalismmod:lpg", "capitalismmod:refinery_gas" -> true;
            default -> false;
        };
    }

    /** Returns the number of physical fuel units required by a recipe batch. */
    public static int requiredUnits(Map<String, Integer> inputs) {
        long total = 0L;
        if (inputs != null) {
            for (Map.Entry<String, Integer> input : inputs.entrySet()) {
                if (isEnergyItem(input.getKey()) && input.getValue() != null && input.getValue() > 0) {
                    total += input.getValue();
                }
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /** Returns available fuel units, without counting ordinary materials. */
    public static int availableUnits(Map<String, Integer> inventory) {
        long total = 0L;
        if (inventory != null) {
            for (Map.Entry<String, Integer> item : inventory.entrySet()) {
                if (isEnergyItem(item.getKey()) && item.getValue() != null && item.getValue() > 0) {
                    total += item.getValue();
                }
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public static boolean hasRequiredFuel(Map<String, Integer> inputs, Map<String, Integer> inventory) {
        if (inputs == null) return true;
        for (Map.Entry<String, Integer> input : inputs.entrySet()) {
            if (!isEnergyItem(input.getKey()) || input.getValue() == null || input.getValue() <= 0) continue;
            if (inventory == null || inventory.getOrDefault(input.getKey(), 0) < input.getValue()) return false;
        }
        return true;
    }
}
