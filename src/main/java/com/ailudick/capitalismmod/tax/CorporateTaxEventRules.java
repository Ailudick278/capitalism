package com.ailudick.capitalismmod.tax;

/** Deterministic source keys for idempotent annual corporate-tax accumulation. */
public final class CorporateTaxEventRules {
    private CorporateTaxEventRules() {}

    public static String income(String companyId, String sourceId) {
        return key("income", companyId, sourceId);
    }

    public static String expense(String companyId, String sourceId) {
        return key("expense", companyId, sourceId);
    }

    private static String key(String kind, String companyId, String sourceId) {
        if (companyId == null || companyId.isBlank() || sourceId == null || sourceId.isBlank()) return "";
        return kind + ":" + companyId + ":" + sourceId;
    }
}
