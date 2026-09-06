package com.ailudick.capitalismmod.population;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicServiceEconomicsTest {
    @Test
    void combinesCoverageAndKeepsPrivateBaseline() {
        assertEquals(100, PublicServiceEconomics.score(0, 0, 0, 0));
        int baseline = PublicServiceEconomics.score(100, 0, 0, 0);
        int covered = PublicServiceEconomics.score(100, 25, 10, 10);
        assertEquals(40, baseline);
        assertEquals(100, covered);
        assertTrue(covered > baseline);
    }
}
