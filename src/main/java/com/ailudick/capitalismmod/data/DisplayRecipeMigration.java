package com.ailudick.capitalismmod.data;

import java.util.Map;

/** Pure compatibility rules for the original one-step display-panel recipe. */
public final class DisplayRecipeMigration {
    private static final Map<String, Integer> LEGACY_INPUTS = Map.of(
            "minecraft:glass", 1,
            "capitalismmod:silicon_wafer", 1,
            "capitalismmod:copper_wire", 1,
            "capitalismmod:plastic_pellets", 1);
    private static final Map<String, Integer> DISPLAY_OUTPUT =
            Map.of("capitalismmod:display_panel", 1);
    private static final Map<String, Integer> UPGRADED_INPUTS = Map.of(
            "capitalismmod:display_glass_substrate", 1,
            "capitalismmod:backlight_module", 1,
            "capitalismmod:display_driver", 1,
            "capitalismmod:plastic_pellets", 1);

    private DisplayRecipeMigration() {
    }

    public static boolean isLegacyDefault(String recipeId,
                                          Map<String, Integer> inputs,
                                          Map<String, Integer> outputs) {
        return "display_panel".equals(recipeId)
                && LEGACY_INPUTS.equals(inputs)
                && DISPLAY_OUTPUT.equals(outputs);
    }

    public static Map<String, Integer> upgradedInputs() {
        return UPGRADED_INPUTS;
    }
}
