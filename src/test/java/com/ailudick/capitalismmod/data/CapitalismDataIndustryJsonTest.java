package com.ailudick.capitalismmod.data;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CapitalismDataIndustryJsonTest {
    @Test
    void positiveIncomeServiceDefaultsToOneWorker() {
        CapitalismData.IndustryJson service = new CapitalismData.IndustryJson(
                "transport", Map.of("minecraft:coal", 1), Map.of(), 45L);
        CapitalismData.IndustryJson finance = new CapitalismData.IndustryJson(
                "finance", Map.of(), Map.of(), 0L);

        assertEquals(1, service.workers_per_cycle);
        assertEquals(0, finance.workers_per_cycle);
    }

    @Test
    void legacyPolymerRecipeOutputIsUpgradedBySpecificRecipeId() {
        CapitalismData.RecipeJson polyethylene = new CapitalismData.RecipeJson(
                "polyethylene_pellets", Map.of("capitalismmod:ethylene", 1),
                Map.of("capitalismmod:plastic_pellets", 3), 78L,
                "polymer_reactor", 2, 4L, 7L);
        CapitalismData.RecipeJson polypropylene = new CapitalismData.RecipeJson(
                "polypropylene_pellets", Map.of("capitalismmod:propylene", 1),
                Map.of("capitalismmod:plastic_pellets", 3), 82L,
                "polymer_reactor", 2, 4L, 7L);

        polyethylene.outputs = PolymerRecipeMigration.upgradeOutputs(polyethylene.id, polyethylene.outputs);
        polypropylene.outputs = PolymerRecipeMigration.upgradeOutputs(polypropylene.id, polypropylene.outputs);

        assertEquals(Map.of("capitalismmod:polyethylene_pellets", 3), polyethylene.outputs);
        assertEquals(Map.of("capitalismmod:polypropylene_pellets", 3), polypropylene.outputs);
    }

    @Test
    void customizedPolymerRecipeOutputIsNotOverwritten() {
        CapitalismData.RecipeJson customized = new CapitalismData.RecipeJson(
                "polyethylene_pellets", Map.of("capitalismmod:ethylene", 1),
                Map.of("capitalismmod:plastic_pellets", 2), 78L,
                "polymer_reactor", 2, 4L, 7L);

        customized.outputs = PolymerRecipeMigration.upgradeOutputs(customized.id, customized.outputs);

        assertNotEquals(Map.of("capitalismmod:polyethylene_pellets", 3), customized.outputs);
        assertEquals(Map.of("capitalismmod:plastic_pellets", 2), customized.outputs);
    }
}
