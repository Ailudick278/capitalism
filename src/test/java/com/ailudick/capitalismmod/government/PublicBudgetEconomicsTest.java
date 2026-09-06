package com.ailudick.capitalismmod.government;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
