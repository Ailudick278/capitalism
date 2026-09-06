package com.ailudick.capitalismmod.data;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SemiconductorRecipeMigrationTest {
    @Test
    void recognizesOnlyTheOriginalChipPackagingRecipe() {
        assertTrue(SemiconductorRecipeMigration.isLegacyChipPackaging(
                "chip_packaging",
                Map.of("capitalismmod:silicon_wafer", 1,
                        "capitalismmod:copper_wire", 1,
                        "capitalismmod:plastic_pellets", 1),
                Map.of("capitalismmod:packaged_chip", 1)));
        assertFalse(SemiconductorRecipeMigration.isLegacyChipPackaging(
                "chip_packaging",
                Map.of("capitalismmod:tested_die", 1,
                        "capitalismmod:lead_frame", 1,
                        "capitalismmod:copper_wire", 1,
                        "capitalismmod:mold_compound", 1),
                Map.of("capitalismmod:packaged_chip", 1)));
    }

    @Test
    void recognizesOnlyTheOriginalCircuitBoardRecipe() {
        assertTrue(SemiconductorRecipeMigration.isLegacyCircuitBoard(
                "circuit_board",
                Map.of("capitalismmod:silicon_wafer", 1,
                        "capitalismmod:copper_wire", 2,
                        "capitalismmod:plastic_pellets", 1),
                Map.of("capitalismmod:circuit_board", 1)));
        assertFalse(SemiconductorRecipeMigration.isLegacyCircuitBoard(
                "circuit_board",
                Map.of("capitalismmod:solder_masked_pcb", 1,
                        "capitalismmod:solder", 1,
                        "capitalismmod:smd_components", 1,
                        "capitalismmod:packaged_chip", 1),
                Map.of("capitalismmod:circuit_board", 1)));
    }
}
