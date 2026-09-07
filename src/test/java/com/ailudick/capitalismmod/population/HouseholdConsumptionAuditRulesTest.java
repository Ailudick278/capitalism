package com.ailudick.capitalismmod.population;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HouseholdConsumptionAuditRulesTest {
    @Test
    void validatesCostAndDailyIdentity() {
        assertTrue(HouseholdConsumptionAuditRules.valid("c-1", "npc-1", 4L, "food",
                "minecraft:wheat", 3L, 200L, 600L));
        assertFalse(HouseholdConsumptionAuditRules.valid("c-1", "npc-1", -1L, "food",
                "minecraft:wheat", 3L, 200L, 600L));
        assertFalse(HouseholdConsumptionAuditRules.valid("c-1", "npc-1", 4L, "food",
                "minecraft:wheat", 3L, 200L, 500L));
        assertTrue(HouseholdConsumptionAuditRules.valid("c-2", "npc-1", 4L, "food",
                "minecraft:wheat", 3L, 226L, 678L, 78L));
        assertFalse(HouseholdConsumptionAuditRules.valid("c-2", "npc-1", 4L, "food",
                "minecraft:wheat", 3L, 226L, 678L, 678L));
    }
}
