package com.ailudick.capitalismmod.population;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HousingEconomicsTest {
    @Test
    void crowdingAddsRentAndDistanceAddsCommuteCost() {
        assertEquals(100L, HousingEconomics.dailyRent(100L, 4, 1));
        assertEquals(150L, HousingEconomics.dailyRent(100L, 6, 1));
        assertEquals(200L, HousingEconomics.commuteCost(100L, 2, 10));
        assertTrue(HousingEconomics.commuteCost(100L, 2, 10)
                > HousingEconomics.commuteCost(100L, 2, 1));
    }

    @Test
    void relocationCostIncludesLivingRentAndFriction() {
        assertEquals(4_000L, HousingEconomics.migrationCost(100L, 2, 50L, 500L));
        assertTrue(HousingEconomics.migrationCost(100L, 2, 50L, 900L)
                > HousingEconomics.migrationCost(100L, 2, 50L, 500L));
    }
}
