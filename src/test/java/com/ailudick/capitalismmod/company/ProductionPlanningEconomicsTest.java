package com.ailudick.capitalismmod.company;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionPlanningEconomicsTest {
    @Test
    void prioritizesBackordersAndAvoidsGlutAtWeakPrices() {
        assertTrue(ProductionPlanningEconomics.shouldRun(4, 20, 2, 100, 100, 1000, 10));
        assertFalse(ProductionPlanningEconomics.shouldRun(0, 4, 2, 100, 100, 1000, 10));
        assertTrue(ProductionPlanningEconomics.shouldRun(0, 4, 2, 120, 100, 1000, 10));
        assertFalse(ProductionPlanningEconomics.shouldRun(0, 0, 2, 100, 100, 5, 10));
    }
}
