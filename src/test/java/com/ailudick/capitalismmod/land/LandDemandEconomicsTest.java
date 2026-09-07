package com.ailudick.capitalismmod.land;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LandDemandEconomicsTest {
    @Test
    void populationAndServicesRaiseReferenceDemandWithinBounds() {
        assertEquals(0.8, LandDemandEconomics.multiplier(0, 0), 0.0001);
        assertTrue(LandDemandEconomics.multiplier(10_000, 100)
                > LandDemandEconomics.multiplier(0, 0));
        assertTrue(LandDemandEconomics.multiplier(Integer.MAX_VALUE, 100, 100) <= 1.95);
        assertTrue(LandDemandEconomics.multiplier(0, 50, 5)
                > LandDemandEconomics.multiplier(0, 50, 0));
        assertTrue(LandDemandEconomics.multiplier(0, 50, 0, 100)
                > LandDemandEconomics.multiplier(0, 50, 0, 0));
        assertEquals(1.0, LandDemandEconomics.marketMultiplier(0, 100.0), 0.0001);
        assertTrue(LandDemandEconomics.marketMultiplier(10_000, 100.0) <= 1.50);
    }
}
