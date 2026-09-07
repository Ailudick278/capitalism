package com.ailudick.capitalismmod.company;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionEnergyEconomicsTest {
    @Test
    void countsOnlyPhysicalFuelInputs() {
        assertEquals(3, ProductionEnergyEconomics.requiredUnits(Map.of(
                "minecraft:raw_iron", 2, "minecraft:coal", 1,
                "capitalismmod:fuel_oil", 2)));
    }

    @Test
    void shortageBlocksBatchBeforeOrdinaryMaterialSettlement() {
        Map<String, Integer> inputs = Map.of("minecraft:coal", 2, "minecraft:iron_ingot", 1);
        assertFalse(ProductionEnergyEconomics.hasRequiredFuel(inputs,
                Map.of("minecraft:coal", 1, "minecraft:iron_ingot", 99)));
        assertTrue(ProductionEnergyEconomics.hasRequiredFuel(inputs,
                Map.of("minecraft:coal", 2)));
        assertFalse(ProductionEnergyEconomics.hasRequiredFuel(inputs,
                Map.of("capitalismmod:diesel", 2)));
    }
}
