package com.ailudick.capitalismmod.market;

import java.util.Map;
import java.util.TreeMap;

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

    public static String batchSignature(Map<String, Integer> requirements) {
        if (requirements == null || requirements.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        for (var entry : new TreeMap<>(requirements).entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue() <= 0) {
                return "";
            }
            if (result.length() > 0) result.append(',');
            result.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return result.toString();
    }

    public static boolean matchesBatch(String recordedSignature, Map<String, Integer> requested) {
        if (recordedSignature == null) return true; // legacy receipt
        return !recordedSignature.isBlank() && recordedSignature.equals(batchSignature(requested));
    }
}
