package com.ailudick.capitalismmod.government;

import java.util.List;
import java.util.Map;

/** Pure fixed-basket price-index arithmetic. Index 10000 means the base-period price level. */
public final class InflationEconomics {
    private InflationEconomics() {}

    public static int weightedIndex(Map<String, Long> current, Map<String, Long> base,
                                    List<String> basket) {
        if (current == null || base == null || basket == null || basket.isEmpty()) return 10000;
        java.util.Map<String, Integer> weights = new java.util.HashMap<>();
        for (String item : basket) weights.put(item, 1);
        return weightedIndex(current, base, weights);
    }

    public static int weightedIndex(Map<String, Long> current, Map<String, Long> base,
                                    Map<String, Integer> weights) {
        if (current == null || base == null || weights == null || weights.isEmpty()) return 10000;
        double total = 0.0;
        int totalWeight = 0;
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            String item = entry.getKey();
            int weight = entry.getValue() == null ? 0 : entry.getValue();
            long now = current.getOrDefault(item, 0L);
            long initial = base.getOrDefault(item, 0L);
            if (weight <= 0 || now <= 0L || initial <= 0L) continue;
            total += (double) now / (double) initial * weight;
            totalWeight += weight;
        }
        if (totalWeight == 0) return 10000;
        double index = total / totalWeight * 10000.0;
        if (!Double.isFinite(index)) return Integer.MAX_VALUE;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, Math.round(index)));
    }

    /** One-day bounded Taylor-style response: 25 bps toward the price-level target. */
    public static int policyRateAdjustment(int currentIndexBps, int targetIndexBps) {
        if (currentIndexBps > targetIndexBps) return 25;
        if (currentIndexBps < targetIndexBps) return -25;
        return 0;
    }

    public static boolean automaticAdjustmentDue(long day, long lastAdjustmentDay) {
        return day >= 0L && day > lastAdjustmentDay;
    }
}
