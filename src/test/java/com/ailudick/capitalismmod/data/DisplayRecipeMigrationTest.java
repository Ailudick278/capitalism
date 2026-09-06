package com.ailudick.capitalismmod.data;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayRecipeMigrationTest {
    private static final Map<String, Integer> LEGACY_INPUTS = Map.of(
            "minecraft:glass", 1,
            "capitalismmod:silicon_wafer", 1,
            "capitalismmod:copper_wire", 1,
            "capitalismmod:plastic_pellets", 1);

    @Test
    void recognizesOnlyUnchangedBuiltInRecipe() {
        assertTrue(DisplayRecipeMigration.isLegacyDefault(
                "display_panel", LEGACY_INPUTS, Map.of("capitalismmod:display_panel", 1)));
        assertFalse(DisplayRecipeMigration.isLegacyDefault(
                "display_panel", Map.of("minecraft:glass", 2), Map.of("capitalismmod:display_panel", 1)));
        assertFalse(DisplayRecipeMigration.isLegacyDefault(
                "custom_display_panel", LEGACY_INPUTS, Map.of("capitalismmod:display_panel", 1)));
    }

    @Test
    void suppliesLayeredDisplayInputs() {
        assertEquals(Map.of(
                "capitalismmod:display_glass_substrate", 1,
                "capitalismmod:backlight_module", 1,
                "capitalismmod:display_driver", 1,
                "capitalismmod:plastic_pellets", 1), DisplayRecipeMigration.upgradedInputs());
    }
}
