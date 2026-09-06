package com.ailudick.capitalismmod.government;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicBudgetEconomicsTest {
    @Test
    void maintenanceScalesByFacilityAndRejectsUnknownTypes() {
        assertEquals(500L, PublicBudgetEconomics.dailyMaintenance("housing", 10));
        assertEquals(3_000L, PublicBudgetEconomics.dailyMaintenance("school", 10));
        assertEquals(5_000L, PublicBudgetEconomics.dailyMaintenance("clinic", 10));
        assertEquals(0L, PublicBudgetEconomics.dailyMaintenance("port", 10));
        assertEquals(0L, PublicBudgetEconomics.dailyMaintenance("clinic", 0));
    }

    @Test
    void maintenanceHandlesLargeCitiesWithoutOverflow() {
        assertEquals(50L * Integer.MAX_VALUE,
                PublicBudgetEconomics.dailyMaintenance("housing", Integer.MAX_VALUE));
        assertEquals(0L, PublicBudgetEconomics.dailyMaintenance(null, Integer.MAX_VALUE));
        assertEquals(0L, PublicBudgetEconomics.dailyMaintenance("clinic", -1));
    }

    @Test
    void essentialServicesHaveExplicitFiscalPriority() {
        assertTrue(PublicBudgetEconomics.priority("housing") > PublicBudgetEconomics.priority("clinic"));
        assertTrue(PublicBudgetEconomics.priority("clinic") > PublicBudgetEconomics.priority("school"));
        assertEquals(0, PublicBudgetEconomics.priority("port"));
    }

    @Test
    void fiscalStressMeasuresSevenDayCoverage() {
        assertEquals(0, PublicBudgetEconomics.fiscalStress(700L, 100L));
        assertEquals(100, PublicBudgetEconomics.fiscalStress(0L, 100L));
        assertTrue(PublicBudgetEconomics.fiscalStress(350L, 100L) > 0);
    }

    @Test
    void highFiscalStressStopsNonEssentialExpansion() {
        assertTrue(PublicBudgetEconomics.allowNonEssentialExpansion(74));
        assertTrue(!PublicBudgetEconomics.allowNonEssentialExpansion(75));
        assertTrue(!PublicBudgetEconomics.allowNonEssentialExpansion(100));
    }

    @Test
    void unknownPriorityDoesNotCreateMaintenanceProtection() {
        assertEquals(0, PublicBudgetEconomics.priority("unknown"));
    }

    @Test
    void laborShockActivatesCappedAutomaticStabilizer() {
        assertEquals(10, PublicBudgetEconomics.effectiveRegionalSupportRate(10, 0));
        assertEquals(15, PublicBudgetEconomics.effectiveRegionalSupportRate(10, -5000));
        assertEquals(30, PublicBudgetEconomics.effectiveRegionalSupportRate(25, -9000));
        assertEquals(10, PublicBudgetEconomics.effectiveRegionalSupportRate(10, 5000));
    }
}
