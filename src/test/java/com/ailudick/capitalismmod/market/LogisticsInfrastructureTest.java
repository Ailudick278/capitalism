package com.ailudick.capitalismmod.market;

import com.ailudick.capitalismmod.population.MigrationEconomics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogisticsInfrastructureTest {
    @Test
    void migrationFrictionFallsAsAccessImproves() {
        long poor = MigrationEconomics.friction(1_000L, 2, 0);
        long connected = MigrationEconomics.friction(1_000L, 2, 50);
        assertEquals(2_000L, poor);
        assertEquals(1_000L, connected);
        assertEquals(0L, MigrationEconomics.friction(1_000L, 2, 100));
        assertTrue(connected < poor);
    }
}
