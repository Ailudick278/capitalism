package com.ailudick.capitalismmod.population;

/** Invariants for durable simulated-household consumption records. */
public final class HouseholdConsumptionAuditRules {
    private HouseholdConsumptionAuditRules() {
    }

    public static boolean valid(String id, String householdId, long day, String category,
                                String itemId, long quantity, long unitPriceMinor, long totalCostMinor) {
        if (id == null || id.isBlank() || householdId == null || householdId.isBlank()
                || day < 0L || category == null || category.isBlank()
                || itemId == null || itemId.isBlank() || quantity <= 0L
                || unitPriceMinor <= 0L || totalCostMinor <= 0L) return false;
        try {
            return Math.multiplyExact(quantity, unitPriceMinor) == totalCostMinor;
        } catch (ArithmeticException exception) {
            return false;
        }
    }
}
