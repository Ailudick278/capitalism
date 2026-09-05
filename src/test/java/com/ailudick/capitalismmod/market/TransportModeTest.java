package com.ailudick.capitalismmod.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportModeTest {
    @Test
    void transportModesExposeDistinctFuelPlanningInputs() {
        assertEquals("capitalismmod:gasoline", TransportMode.ROAD.fuelItemId());
        assertEquals("capitalismmod:diesel", TransportMode.RAIL.fuelItemId());
        assertEquals("capitalismmod:fuel_oil", TransportMode.SEA.fuelItemId());
        assertTrue(TransportMode.SEA.estimatedFuelUnits(512, 12)
                > TransportMode.ROAD.estimatedFuelUnits(512, 2));
    }

    @Test
    void zeroDistanceDoesNotRequireFuel() {
        assertEquals(0, TransportMode.ROAD.estimatedFuelUnits(100, 0));
        assertEquals(0, TransportMode.SEA.estimatedFuelUnits(0, 10));
    }
}
