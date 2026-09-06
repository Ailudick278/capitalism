package com.ailudick.capitalismmod.company;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FifoInventoryCostTest {
    @Test
    void consumesOldestBatchFirst() {
        FifoInventoryCost.Consumption result = FifoInventoryCost.consume(
                List.of(new FifoInventoryCost.Batch(5, 50), new FifoInventoryCost.Batch(5, 100)), 6);

        assertEquals(6, result.quantity());
        assertEquals(70, result.cost());
        assertEquals(List.of(new FifoInventoryCost.Batch(4, 80)), result.remaining());
    }

    @Test
    void allocatesPartialBatchCostProportionally() {
        FifoInventoryCost.Consumption result = FifoInventoryCost.consume(
                List.of(new FifoInventoryCost.Batch(3, 101)), 1);

        assertEquals(1, result.quantity());
        assertEquals(33, result.cost());
        assertEquals(List.of(new FifoInventoryCost.Batch(2, 68)), result.remaining());
    }
}
