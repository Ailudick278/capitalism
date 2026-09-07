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
        assertTrue(LandDemandEconomics.multiplier(Integer.MAX_VALUE, 100) <= 1.7);
    }
}
