package com.ailudick.capitalismmod.company;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryValuationTest {
    @Test
    void recordsWriteDownWhenNrvFallsBelowCost() {
        InventoryValuation.Result result = InventoryValuation.lowerOfCostAndNrv(100L, 70L);

        assertEquals(70L, result.carryingValue());
        assertEquals(30L, result.writeDown());
    }

    @Test
    void doesNotWriteDownWhenNrvExceedsCost() {
        InventoryValuation.Result result = InventoryValuation.lowerOfCostAndNrv(70L, 100L);

        assertEquals(70L, result.carryingValue());
        assertEquals(0L, result.writeDown());
    }
}
