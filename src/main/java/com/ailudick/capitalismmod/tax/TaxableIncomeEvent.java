package com.ailudick.capitalismmod.tax;

/** A single business income event that may be assessed exactly once. */
public record TaxableIncomeEvent(String eventId, TaxSubject subject, long revenue,
                                 String currencyId, long occurredAt) {
    public TaxableIncomeEvent {
        if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("Income event id is required");
        if (subject == null) throw new IllegalArgumentException("Tax subject is required");
        if (revenue <= 0L) throw new IllegalArgumentException("Revenue must be positive");
    }
}
