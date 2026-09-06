package com.ailudick.capitalismmod.company;

/** Transparent inventory-and-demand rule for automatic production scheduling. */
public final class ProductionPlanningEconomics {
    private ProductionPlanningEconomics() {}

    public static boolean shouldRun(int backlogUnits, int inventoryUnits, int batchOutput,
                                    long marketPrice, long fundamentalPrice,
                                    long treasury, long estimatedOperatingCost) {
        if (treasury < Math.max(0L, estimatedOperatingCost)) return false;
        if (backlogUnits > 0) return true;
        long safetyStock = Math.max(1L, (long) Math.max(1, batchOutput) * 2L);
        return inventoryUnits < safetyStock || marketPrice > fundamentalPrice;
    }
}
