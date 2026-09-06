package com.ailudick.capitalismmod.company;

/** Stable identity helpers shared by production, tax and audit records. */
public final class ProductionCycleIdentity {
    private ProductionCycleIdentity() {}

    public static String key(String companyId, long cycleTick, int batchIndex) {
        if (companyId == null || companyId.isBlank() || cycleTick < 0L || batchIndex < 0) return "";
        return companyId + ":" + cycleTick + ":" + batchIndex;
    }

    public static String batchId(String cycleKey) {
        return cycleKey == null || cycleKey.isBlank() ? "" : "production:" + cycleKey;
    }
}
