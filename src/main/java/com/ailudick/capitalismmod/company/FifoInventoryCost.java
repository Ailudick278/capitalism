package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.util.EconomyMath;

import java.util.ArrayList;
import java.util.List;

/** Pure FIFO inventory-cost allocation for interchangeable inventory. */
public final class FifoInventoryCost {
    private FifoInventoryCost() {
    }

    public record Batch(int quantity, long totalCost) {
        public Batch {
            quantity = Math.max(0, quantity);
            totalCost = Math.max(0L, totalCost);
        }
    }

    public record Consumption(int quantity, long cost, List<Batch> remaining) {
        public Consumption {
            quantity = Math.max(0, quantity);
            cost = Math.max(0L, cost);
            remaining = List.copyOf(remaining == null ? List.of() : remaining);
        }
    }

    public static List<Batch> add(List<Batch> existing, int quantity, long totalCost) {
        List<Batch> result = new ArrayList<>(existing == null ? List.of() : existing);
        if (quantity > 0 && totalCost >= 0L) result.add(new Batch(quantity, totalCost));
        return List.copyOf(result);
    }

    public static Consumption consume(List<Batch> existing, int requested) {
        if (requested <= 0 || existing == null || existing.isEmpty()) {
            return new Consumption(0, 0L, existing == null ? List.of() : existing);
        }
        List<Batch> remaining = new ArrayList<>();
        int left = requested;
        int taken = 0;
        long cost = 0L;
        for (Batch batch : existing) {
            if (batch == null || batch.quantity() <= 0) continue;
            if (left <= 0) {
                remaining.add(batch);
                continue;
            }
            int use = Math.min(left, batch.quantity());
            long batchCost = use == batch.quantity()
                    ? batch.totalCost() : proportionalCost(batch.totalCost(), use, batch.quantity());
            taken += use;
            left -= use;
            cost = addSaturated(cost, batchCost);
            int rest = batch.quantity() - use;
            long remainingCost = Math.max(0L, batch.totalCost() - Math.min(batch.totalCost(), batchCost));
            if (rest > 0) remaining.add(new Batch(rest, remainingCost));
        }
        return new Consumption(taken, cost, remaining);
    }

    public static int quantity(List<Batch> batches) {
        int total = 0;
        if (batches != null) for (Batch batch : batches) {
            if (batch != null && batch.quantity() > 0) {
                total = batch.quantity() > Integer.MAX_VALUE - total
                        ? Integer.MAX_VALUE : total + batch.quantity();
            }
        }
        return total;
    }

    public static long totalCost(List<Batch> batches) {
        long total = 0L;
        if (batches != null) for (Batch batch : batches) {
            if (batch != null && batch.quantity() > 0) total = addSaturated(total, batch.totalCost());
        }
        return total;
    }

    private static long proportionalCost(long total, int quantity, int denominator) {
        if (total <= 0L || quantity <= 0 || denominator <= 0) return 0L;
        if (total == Long.MAX_VALUE) return Long.MAX_VALUE;
        long whole = total / denominator;
        long remainder = total % denominator;
        long result = EconomyMath.add(EconomyMath.multiply(whole, quantity),
                EconomyMath.multiply(remainder, quantity) / denominator);
        return result < 0L ? Long.MAX_VALUE : result;
    }

    private static long addSaturated(long left, long right) {
        long result = EconomyMath.add(Math.max(0L, left), Math.max(0L, right));
        return result < 0L ? Long.MAX_VALUE : result;
    }
}
