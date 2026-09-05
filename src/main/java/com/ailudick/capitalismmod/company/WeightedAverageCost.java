package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.util.EconomyMath;

/** Pure weighted-average inventory cost calculation, independent of Minecraft persistence. */
public record WeightedAverageCost(int quantity, long totalCost) {
    public WeightedAverageCost {
        quantity = Math.max(0, quantity);
        totalCost = Math.max(0L, totalCost);
    }

    public WeightedAverageCost add(int addedQuantity, long addedCost) {
        if (addedQuantity <= 0 || addedCost < 0L) return this;
        int nextQuantity = addedQuantity > Integer.MAX_VALUE - quantity
                ? Integer.MAX_VALUE : quantity + addedQuantity;
        long nextCost = EconomyMath.add(totalCost, addedCost);
        return new WeightedAverageCost(nextQuantity, nextCost < 0L ? Long.MAX_VALUE : nextCost);
    }

    public Consumption consume(int requested) {
        if (requested <= 0 || quantity <= 0) return new Consumption(0, 0L, this);
        int taken = Math.min(requested, quantity);
        long cost = taken == quantity ? totalCost : proportionalCost(totalCost, taken, quantity);
        return new Consumption(taken, cost,
                new WeightedAverageCost(quantity - taken, Math.max(0L, totalCost - Math.min(totalCost, cost))));
    }

    private static long proportionalCost(long total, int quantity, int denominator) {
        if (total <= 0L || quantity <= 0 || denominator <= 0) return 0L;
        if (total == Long.MAX_VALUE) return Long.MAX_VALUE;
        long whole = total / denominator;
        long remainder = total % denominator;
        long result = EconomyMath.multiply(whole, quantity);
        result = EconomyMath.add(result, EconomyMath.multiply(remainder, quantity) / denominator);
        return result < 0L ? Long.MAX_VALUE : result;
    }

    public record Consumption(int quantity, long cost, WeightedAverageCost remaining) {
    }
}
