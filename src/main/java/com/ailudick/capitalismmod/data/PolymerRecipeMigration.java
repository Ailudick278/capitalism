package com.ailudick.capitalismmod.data;

import java.util.HashMap;
import java.util.Map;

/** Pure compatibility rules for first-generation polymer recipe data. */
final class PolymerRecipeMigration {
    private PolymerRecipeMigration() {
    }

    static Map<String, Integer> upgradeOutputs(String recipeId, Map<String, Integer> outputs) {
        if (recipeId == null || outputs == null) return outputs;
        if (Map.of("capitalismmod:plastic_pellets", 3).equals(outputs)) {
            if ("polyethylene_pellets".equals(recipeId)) {
                return new HashMap<>(Map.of("capitalismmod:polyethylene_pellets", 3));
            }
            if ("polypropylene_pellets".equals(recipeId)) {
                return new HashMap<>(Map.of("capitalismmod:polypropylene_pellets", 3));
            }
        }
        return outputs;
    }
}
