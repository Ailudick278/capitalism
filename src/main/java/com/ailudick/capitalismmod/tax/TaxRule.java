package com.ailudick.capitalismmod.tax;

/** Versioned tax rule used by all tax-producing systems. Amounts are minor currency units. */
public record TaxRule(TaxType type, int rateBasisPoints, long thresholdMinor,
                      long exemptionMinor, long effectiveFrom, boolean enabled,
                      String versionId, long createdAt, String createdBy) {
    public TaxRule {
        if (type == null) throw new IllegalArgumentException("Tax type is required");
        rateBasisPoints = Math.max(0, Math.min(1_000_000, rateBasisPoints));
        thresholdMinor = Math.max(0L, thresholdMinor);
        exemptionMinor = Math.max(0L, exemptionMinor);
        effectiveFrom = Math.max(0L, effectiveFrom);
        versionId = versionId == null || versionId.isBlank() ? java.util.UUID.randomUUID().toString() : versionId;
        createdAt = Math.max(0L, createdAt);
        createdBy = createdBy == null || createdBy.isBlank() ? "system" : createdBy;
    }

    public TaxRule(TaxType type, int rateBasisPoints, long thresholdMinor,
                   long exemptionMinor, long effectiveFrom, boolean enabled) {
        this(type, rateBasisPoints, thresholdMinor, exemptionMinor, effectiveFrom, enabled,
                java.util.UUID.randomUUID().toString(), effectiveFrom, "system");
    }

    public double rate() { return rateBasisPoints / 10_000.0; }

    public long taxableBase(long baseMinor) {
        if (!enabled || baseMinor < thresholdMinor) return 0L;
        return Math.max(0L, baseMinor - exemptionMinor);
    }
}
