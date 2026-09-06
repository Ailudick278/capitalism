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

    @Test
    void qualityRequiresLaborAndImprovesWithSkill() {
        assertEquals(0, PublicConstructionEconomics.qualityScore(80, 0));
        assertEquals(64, PublicConstructionEconomics.qualityScore(40, 2));
        assertTrue(PublicConstructionEconomics.qualityScore(80, 1)
                > PublicConstructionEconomics.qualityScore(40, 1));
    }

    @Test
    void reworkIsChargedAsAQuarterOfOriginalUnitCost() {
        assertEquals(75_000L, PublicConstructionEconomics.reworkCost(300_000L));
        assertEquals(0L, PublicConstructionEconomics.reworkCost(0L));
    }
}
