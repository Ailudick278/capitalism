package com.ailudick.capitalismmod.tax;

/** A closed interval in game time used for one tax calculation cycle. */
public record TaxPeriod(long startAt, long endAt, long declarationDueAt, long paymentDueAt) {
    public TaxPeriod {
        if (endAt < startAt) throw new IllegalArgumentException("Tax period end must not precede start");
        if (declarationDueAt < endAt) throw new IllegalArgumentException("Declaration deadline must follow period end");
        if (paymentDueAt < declarationDueAt) throw new IllegalArgumentException("Payment deadline must follow declaration deadline");
    }

    public boolean ended(long now) { return now >= endAt; }
    public boolean declarationOverdue(long now) { return now > declarationDueAt; }
    public boolean paymentOverdue(long now) { return now > paymentDueAt; }
}
