package com.ailudick.capitalismmod.company;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionCycleIdentityTest {
    @Test
    void buildsStableCycleAndBatchIds() {
        String key = ProductionCycleIdentity.key("company-1", 1200L, 2);

        assertEquals("company-1:1200:2", key);
        assertEquals("production:company-1:1200:2", ProductionCycleIdentity.batchId(key));
    }

    @Test
    void rejectsInvalidIdentityParts() {
        assertTrue(ProductionCycleIdentity.key("", 1200L, 0).isEmpty());
        assertTrue(ProductionCycleIdentity.key("company-1", -1L, 0).isEmpty());
        assertTrue(ProductionCycleIdentity.key("company-1", 1200L, -1).isEmpty());
        assertTrue(ProductionCycleIdentity.batchId("").isEmpty());
    }
}
