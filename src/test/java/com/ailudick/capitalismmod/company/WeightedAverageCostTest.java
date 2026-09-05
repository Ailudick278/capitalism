package com.ailudick.capitalismmod.company;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeightedAverageCostTest {
    @Test
    void consumesWeightedAverageCost() {
        WeightedAverageCost cost = new WeightedAverageCost(10, 100L)
                .add(10, 300L);

        WeightedAverageCost.Consumption first = cost.consume(5);
        WeightedAverageCost.Consumption remainder = first.remaining().consume(15);

        assertEquals(5, first.quantity());
        assertEquals(100L, first.cost());
        assertEquals(15, remainder.quantity());
        assertEquals(300L, remainder.cost());
    }
}
