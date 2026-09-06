package com.ailudick.capitalismmod.government;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InflationEconomicsTest {
    @Test
    void fixedBasketUsesBasePeriodAndEqualWeights() {
        assertEquals(10000, InflationEconomics.weightedIndex(
                Map.of("food", 120L, "energy", 80L),
                Map.of("food", 100L, "energy", 100L), List.of("food", "energy")));
        assertEquals(10000, InflationEconomics.weightedIndex(Map.of(), Map.of(), List.of()));
        assertEquals(25, InflationEconomics.policyRateAdjustment(10201, 10200));
        assertEquals(-25, InflationEconomics.policyRateAdjustment(10199, 10200));
        assertEquals(0, InflationEconomics.policyRateAdjustment(10200, 10200));
    }
}
