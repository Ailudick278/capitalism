package com.ailudick.capitalismmod.company;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionRecipeTest {
    @Test
    void positiveIncomeWithoutItemsIsAServiceCycle() {
        ProductionRecipe service = new ProductionRecipe("transport", Map.of(), Map.of(), 45L);
        ProductionRecipe product = new ProductionRecipe("rail", Map.of(), Map.of("minecraft:rail", 1), 80L);
        ProductionRecipe nonRevenue = new ProductionRecipe("finance", Map.of(), Map.of(), 0L);

        assertTrue(service.isService());
        assertFalse(product.isService());
        assertFalse(nonRevenue.isService());
    }
}
