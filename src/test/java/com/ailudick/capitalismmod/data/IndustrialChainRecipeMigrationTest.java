package com.ailudick.capitalismmod.data;

import com.ailudick.capitalismmod.data.CapitalismData.RecipeJson;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class IndustrialChainRecipeMigrationTest {
    private RecipeJson original() {
        return new RecipeJson("bauxite_refining",
                Map.of("capitalismmod:raw_bauxite", 2, "minecraft:coal", 1),
                Map.of("capitalismmod:aluminum_ingot", 1), 82, "blast_furnace", 2, 4, 6);
    }

    private RecipeJson replacement() {
        return new RecipeJson("bauxite_refining",
                Map.of("capitalismmod:raw_bauxite", 2, "capitalismmod:caustic_soda", 1,
                        "minecraft:water_bucket", 1),
                Map.of("capitalismmod:alumina", 1), 82, "ore_processor", 2, 4, 6);
    }

    @Test
    void upgradesOnceAndDoesNotShareMutableMapsWithDefaults() {
        RecipeJson configured = original(), maintained = replacement();
        assertTrue(IndustrialChainRecipeMigration.upgrade("mining", configured, List.of(maintained)));
        assertEquals(maintained.inputs, configured.inputs);
        assertEquals(maintained.outputs, configured.outputs);
        assertEquals("ore_processor", configured.machine_type);
        assertEquals("bauxite_refining", configured.id);
        assertFalse(IndustrialChainRecipeMigration.upgrade("mining", configured, List.of(maintained)));
        configured.inputs.clear();
        assertEquals(3, maintained.inputs.size());
    }

    @Test
    void preservesEveryKindOfPackCustomization() {
        List<Consumer<RecipeJson>> customizations = List.of(
                r -> r.id = "custom_bauxite",
                r -> r.inputs.put("minecraft:coal", 2),
                r -> r.outputs.put("capitalismmod:aluminum_ingot", 2),
                r -> r.income++, r -> r.machine_type = "chemical_reactor",
                r -> r.workers_per_cycle++, r -> r.energy_cost++, r -> r.maintenance_cost++,
                r -> r.material_class = "custom", r -> r.quality_tier = "high_purity",
                r -> r.byproducts.put("minecraft:gravel", 1), r -> r.pollution_score++,
                r -> r.hazardous = true);
        for (Consumer<RecipeJson> customize : customizations) {
            RecipeJson configured = original();
            customize.accept(configured);
            Map<String, Integer> inputsBefore = Map.copyOf(configured.inputs);
            assertFalse(IndustrialChainRecipeMigration.upgrade("mining", configured, List.of(replacement())));
            assertEquals(inputsBefore, configured.inputs);
        }
    }

    @Test
    void requiresBothTheOriginalIndustryAndAnAvailableReplacement() {
        assertFalse(IndustrialChainRecipeMigration.upgrade("custom_mining", original(), List.of(replacement())));
        assertFalse(IndustrialChainRecipeMigration.upgrade("mining", original(), List.of()));
        assertFalse(IndustrialChainRecipeMigration.upgrade("mining", null, List.of(replacement())));
    }

    @Test
    void oldJsonReceivesDefaultMetadataAndMigratesAfterDeserialization() {
        RecipeJson configured = new com.google.gson.Gson().fromJson("""
                {"id":"bauxite_refining","inputs":{"capitalismmod:raw_bauxite":2,"minecraft:coal":1},
                "outputs":{"capitalismmod:aluminum_ingot":1},"income":82,
                "machine_type":"blast_furnace","workers_per_cycle":2,"energy_cost":4,"maintenance_cost":6}
                """, RecipeJson.class);
        assertTrue(IndustrialChainRecipeMigration.upgrade("mining", configured, List.of(replacement())));
        RecipeJson reloaded = new com.google.gson.Gson().fromJson(
                new com.google.gson.Gson().toJson(configured), RecipeJson.class);
        assertEquals(replacement().outputs, reloaded.outputs);
        assertFalse(IndustrialChainRecipeMigration.upgrade("mining", reloaded, List.of(replacement())));
    }

    @Test
    void recognizesPreviouslyMigratedCircuitBoards() {
        RecipeJson configured = new RecipeJson("circuit_board",
                SemiconductorRecipeMigration.upgradedBoardInputs(), Map.of("capitalismmod:circuit_board", 1),
                390, "pcb_assembly_line", 4, 8, 15);
        RecipeJson maintained = new RecipeJson("circuit_board", Map.of("capitalismmod:assembled_pcb", 1),
                Map.of("capitalismmod:circuit_board", 1), 260, "chip_testing_station", 3, 5, 10);
        assertTrue(IndustrialChainRecipeMigration.upgrade("manufacturing", configured, List.of(maintained)));
        assertEquals(Map.of("capitalismmod:assembled_pcb", 1), configured.inputs);
    }
}
