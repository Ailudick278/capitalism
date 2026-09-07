package com.ailudick.capitalismmod.population;

/** Invariants for durable simulated-household consumption records. */
public final class HouseholdConsumptionAuditRules {
    private HouseholdConsumptionAuditRules() {
    }

    public static boolean valid(String id, String householdId, long day, String category,
                                String itemId, long quantity, long unitPriceMinor, long totalCostMinor) {
        return valid(id, householdId, day, category, itemId, quantity, unitPriceMinor,
                totalCostMinor, 0L);
    }

    public static boolean valid(String id, String householdId, long day, String category,
                                String itemId, long quantity, long unitPriceMinor,
                                long totalCostMinor, long taxMinor) {
        if (id == null || id.isBlank() || householdId == null || householdId.isBlank()
                || day < 0L || category == null || category.isBlank()
                || itemId == null || itemId.isBlank() || quantity <= 0L
                || unitPriceMinor <= 0L || totalCostMinor <= 0L || taxMinor < 0L
                || taxMinor >= totalCostMinor) return false;
        try {
            return Math.multiplyExact(quantity, unitPriceMinor) == totalCostMinor;
        } catch (ArithmeticException exception) {
            return false;
        }
    }
}
