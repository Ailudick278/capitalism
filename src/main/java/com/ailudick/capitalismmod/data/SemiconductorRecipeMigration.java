package com.ailudick.capitalismmod.data;

import java.util.Map;

/** Compatibility rules for the original one-step semiconductor recipes. */
public final class SemiconductorRecipeMigration {
    private static final Map<String, Integer> PACKAGED_CHIP_OUTPUT =
            Map.of("capitalismmod:packaged_chip", 1);
    private static final Map<String, Integer> LEGACY_CHIP_INPUTS = Map.of(
            "capitalismmod:silicon_wafer", 1,
            "capitalismmod:copper_wire", 1,
            "capitalismmod:plastic_pellets", 1);
    private static final Map<String, Integer> UPGRADED_CHIP_INPUTS = Map.of(
            "capitalismmod:tested_die", 1,
            "capitalismmod:lead_frame", 1,
            "capitalismmod:copper_wire", 1,
            "capitalismmod:mold_compound", 1);
    private static final Map<String, Integer> CIRCUIT_BOARD_OUTPUT =
            Map.of("capitalismmod:circuit_board", 1);
    private static final Map<String, Integer> LEGACY_BOARD_INPUTS = Map.of(
            "capitalismmod:silicon_wafer", 1,
            "capitalismmod:copper_wire", 2,
            "capitalismmod:plastic_pellets", 1);
    private static final Map<String, Integer> UPGRADED_BOARD_INPUTS = Map.of(
            "capitalismmod:solder_masked_pcb", 1,
            "capitalismmod:solder", 1,
            "capitalismmod:smd_components", 1,
            "capitalismmod:packaged_chip", 1);

    private SemiconductorRecipeMigration() {
    }

    public static boolean isLegacyChipPackaging(String recipeId,
                                                 Map<String, Integer> inputs,
                                                 Map<String, Integer> outputs) {
        return "chip_packaging".equals(recipeId)
                && LEGACY_CHIP_INPUTS.equals(inputs)
                && PACKAGED_CHIP_OUTPUT.equals(outputs);
    }

    public static boolean isLegacyCircuitBoard(String recipeId,
                                               Map<String, Integer> inputs,
                                               Map<String, Integer> outputs) {
        return "circuit_board".equals(recipeId)
                && LEGACY_BOARD_INPUTS.equals(inputs)
                && CIRCUIT_BOARD_OUTPUT.equals(outputs);
    }

    public static Map<String, Integer> upgradedChipInputs() {
        return UPGRADED_CHIP_INPUTS;
    }

    public static Map<String, Integer> upgradedBoardInputs() {
        return UPGRADED_BOARD_INPUTS;
    }
}
