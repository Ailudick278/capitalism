package com.ailudick.capitalismmod.company;

/** Lower-of-cost-and-NRV inventory valuation, independent of Minecraft state. */
public final class InventoryValuation {
    private InventoryValuation() {
    }

    public record Result(long carryingValue, long writeDown) {
        public Result {
            carryingValue = Math.max(0L, carryingValue);
            writeDown = Math.max(0L, writeDown);
        }
    }

    public static Result lowerOfCostAndNrv(long cost, long netRealisableValue) {
        long safeCost = Math.max(0L, cost);
        long safeNrv = Math.max(0L, netRealisableValue);
        long carrying = Math.min(safeCost, safeNrv);
        return new Result(carrying, safeCost - carrying);
    }
}
