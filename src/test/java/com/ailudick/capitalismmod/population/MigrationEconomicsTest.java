package com.ailudick.capitalismmod.population;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MigrationEconomicsTest {
    @Test
    void costOfLivingReflectsRegionalPricePremium() {
        assertEquals(1_000L, MigrationEconomics.costOfLiving(1_000L, 0));
        assertEquals(1_200L, MigrationEconomics.costOfLiving(1_000L, 2_000));
        assertEquals(2_000L, MigrationEconomics.costOfLiving(1_000L, 10_000));
    }
}
