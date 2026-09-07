package com.ailudick.capitalismmod.market;

/** Pure retry rules for warehouse consumption receipts. */
public final class WarehouseAuditRules {
    private WarehouseAuditRules() {
    }

    public static boolean matchesConsumption(String recordedItem, Integer recordedCount,
                                             String requestedItem, int requestedCount) {
        if (recordedItem == null || recordedCount == null) return true; // legacy receipt
        return requestedItem != null && recordedItem.equals(requestedItem)
                && recordedCount == requestedCount && requestedCount > 0;
    }
}
