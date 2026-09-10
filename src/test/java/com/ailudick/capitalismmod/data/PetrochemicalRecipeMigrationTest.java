package com.ailudick.capitalismmod.data;

import com.ailudick.capitalismmod.data.CapitalismData.RecipeJson;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PetrochemicalRecipeMigrationTest {
    private RecipeJson oldRefining() {
        return new RecipeJson("refining", Map.of("capitalismmod:crude_oil", 2), Map.of(
                "capitalismmod:naphtha", 1, "capitalismmod:fuel_oil", 1, "capitalismmod:diesel", 1,
                "capitalismmod:gasoline", 1, "capitalismmod:lpg", 1, "capitalismmod:base_oil", 1,
                "capitalismmod:asphalt", 1), 130, "oil_refinery", 4, 8, 14);
    }

    private RecipeJson newRefining() {
        return new RecipeJson("refining", Map.of("capitalismmod:crude_oil", 3), Map.of(
                "capitalismmod:refinery_gas", 1, "capitalismmod:naphtha", 2, "capitalismmod:kerosene", 1,
                "capitalismmod:gas_oil", 1, "capitalismmod:fuel_oil", 1), 130, "distillation_column", 4, 8, 14);
    }

    @Test
    void oldRefineryNowProducesFractionsAndKeepsItsSelectedRecipeId() {
        RecipeJson configured = oldRefining();
        assertTrue(IndustrialChainRecipeMigration.upgrade("manufacturing", configured, List.of(newRefining())));
        assertEquals(newRefining().outputs, configured.outputs);
        assertEquals(3, configured.inputs.get("capitalismmod:crude_oil"));
        assertEquals("distillation_column", configured.machine_type);
        assertEquals("refining", configured.id);
        assertFalse(IndustrialChainRecipeMigration.upgrade("manufacturing", configured, List.of(newRefining())));
    }

    @Test
    void customRefineryYieldAndSameIdInAnotherIndustryArePreserved() {
        RecipeJson configured = oldRefining();
        configured.outputs.put("capitalismmod:diesel", 2);
        assertFalse(IndustrialChainRecipeMigration.upgrade("manufacturing", configured, List.of(newRefining())));
        assertEquals(2, configured.outputs.get("capitalismmod:diesel"));
        assertFalse(IndustrialChainRecipeMigration.upgrade("petrochemical_refining", oldRefining(), List.of(newRefining())));
    }

    @Test
    void crackingMigratesTheEntireCoproductSetAndWaterInputTogether() {
        RecipeJson configured = new RecipeJson("ethylene_dichloride_cracking",
                Map.of("capitalismmod:ethylene_dichloride", 1),
                Map.of("capitalismmod:vinyl_chloride", 1, "capitalismmod:hydrogen", 1),
                155, "vinyl_chloride_unit", 3, 7, 11);
        RecipeJson maintained = new RecipeJson("ethylene_dichloride_cracking",
                Map.of("capitalismmod:ethylene_dichloride", 1, "minecraft:water_bucket", 1),
                Map.of("capitalismmod:vinyl_chloride", 1, "capitalismmod:hydrochloric_acid", 1),
                155, "vinyl_chloride_unit", 3, 7, 11);
        assertTrue(IndustrialChainRecipeMigration.upgrade("petrochemical_refining", configured, List.of(maintained)));
        assertFalse(configured.outputs.containsKey("capitalismmod:hydrogen"));
        assertEquals(1, configured.outputs.get("capitalismmod:hydrochloric_acid"));
        assertEquals(1, configured.inputs.get("minecraft:water_bucket"));
    }

    @Test
    void customizedPollutionAndByproductsPreventChemicalRecipeReplacement() {
        for (boolean pollution : List.of(true, false)) {
            RecipeJson configured = oldRefining();
            if (pollution) configured.pollution_score = 6;
            else configured.byproducts.put("capitalismmod:sulfur", 1);
            assertFalse(IndustrialChainRecipeMigration.upgrade("manufacturing", configured, List.of(newRefining())));
            assertEquals("oil_refinery", configured.machine_type);
        }
    }
}
