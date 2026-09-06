package com.ailudick.capitalismmod.government;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicConstructionEconomicsTest {
    @Test
    void costsReflectDifferentPublicFacilityIntensity() {
        assertEquals(50_000L, PublicConstructionEconomics.unitCost("housing"));
        assertEquals(300_000L, PublicConstructionEconomics.unitCost("school"));
        assertEquals(500_000L, PublicConstructionEconomics.unitCost("clinic"));
        assertTrue(PublicConstructionEconomics.unitCost("clinic") > PublicConstructionEconomics.unitCost("school"));
    }

    @Test
    void unknownFacilitiesCannotBecomeConstructionProjects() {
        assertFalse(PublicConstructionEconomics.validFacility("port"));
        assertEquals(0L, PublicConstructionEconomics.unitCost("port"));
    }

    @Test
    void materialBillsScaleWithFacilityComplexity() {
        assertEquals(2, PublicConstructionEconomics.materials("housing").size());
        assertTrue(PublicConstructionEconomics.materials("clinic").get("minecraft:stone")
                > PublicConstructionEconomics.materials("housing").get("minecraft:stone"));
        assertTrue(PublicConstructionEconomics.materials("clinic").containsKey("minecraft:iron_ingot"));
    }

    @Test
    void onlyConstructionRolesProvideBuildingCapacity() {
        assertTrue(PublicConstructionEconomics.isConstructionRole("construction"));
        assertTrue(PublicConstructionEconomics.isConstructionRole("建筑工"));
        assertFalse(PublicConstructionEconomics.isConstructionRole("transport"));
    }
}
