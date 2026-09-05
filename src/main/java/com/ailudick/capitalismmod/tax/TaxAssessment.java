package com.ailudick.capitalismmod.tax;

/** Immutable snapshot of a tax calculation before it becomes a payable bill. */
public record TaxAssessment(String id, TaxSubject subject, TaxPeriod period,
                            long taxableBase, int rateBasisPoints, long assessedAmount,
                            String sourceEventId) {
    public TaxAssessment {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Assessment id is required");
        if (subject == null || period == null) throw new IllegalArgumentException("Assessment context is required");
        if (taxableBase < 0L || assessedAmount < 0L) throw new IllegalArgumentException("Assessment amounts cannot be negative");
        rateBasisPoints = Math.max(0, rateBasisPoints);
        sourceEventId = sourceEventId == null ? "" : sourceEventId;
    }
}
