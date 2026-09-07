package com.ailudick.capitalismmod.government;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CityStatisticsAuditRulesTest {
    @Test
    void validatesRegionalIndicatorBounds() {
        var valid = new CityStatisticsSavedData.Snapshot(1L, "spawn", 10, 2, 1, 1,
                80, 5, 100L, 1000L, 10L, 0, 1, 0);
        var invalid = new CityStatisticsSavedData.Snapshot(1L, "spawn", 10, 2, 1, 1,
                101, 5, 100L, 1000L, 10L, 0, 1, 0);
        assertTrue(CityStatisticsAuditRules.valid(valid));
        assertFalse(CityStatisticsAuditRules.valid(invalid));
    }
}
