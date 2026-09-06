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
        assertEquals(11000, InflationEconomics.weightedIndex(
                Map.of("food", 120L, "energy", 100L),
                Map.of("food", 100L, "energy", 100L), Map.of("food", 50, "energy", 50)));
        assertEquals(11000, InflationEconomics.weightedIndex(
                Map.of("foodA", 120L, "foodB", 120L, "energy", 100L),
                Map.of("foodA", 100L, "foodB", 100L, "energy", 100L),
                Map.of("foodA", 2500, "foodB", 2500, "energy", 5000)));
        assertEquals(25, InflationEconomics.policyRateAdjustment(11000, 10000, 500));
        assertEquals(-25, InflationEconomics.policyRateAdjustment(10100, 10000, 167));
        assertEquals(0, InflationEconomics.policyRateAdjustment(10167, 10000, 167));
        assertEquals(true, InflationEconomics.automaticAdjustmentDue(10, 9));
        assertEquals(false, InflationEconomics.automaticAdjustmentDue(10, 10));
    }
}
