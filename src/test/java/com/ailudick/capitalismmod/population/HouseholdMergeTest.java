package com.ailudick.capitalismmod.population;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HouseholdMergeTest {
    @Test
    void mergePreservesPopulationAndCombinesCohorts() {
        Household left = new Household("npc-a", "spawn", 2, 1, 100L, 100L, 80,
                4L, -1L, 80, 20, 2, 5, 30, 1, 0);
        Household right = new Household("npc-b", "spawn", 3, 2, 200L, 200L, 40,
                5L, -1L, 60, 40, 6, 2, 40, 1, 0);

        Household merged = left.mergeWith(right);

        assertNotNull(merged);
        assertEquals(5, merged.size());
        assertEquals(3, merged.workingAge());
        assertEquals(2, merged.children());
        assertEquals(300L, merged.cashMinor());
        assertEquals(5, merged.lastSettlementDay());
    }

    @Test
    void mergeRejectsDifferentRegionsAndSameIdentity() {
        Household base = new Household("npc-a", "spawn", 1, 1, 1L, 1L, 50, 1L);
        assertNull(base.mergeWith(new Household("npc-b", "desert", 1, 1, 1L, 1L, 50, 1L)));
        assertNull(base.mergeWith(base));
    }
}
