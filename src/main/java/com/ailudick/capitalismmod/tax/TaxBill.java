package com.ailudick.capitalismmod.tax;

/** Persistent, auditable tax liability. Monetary amounts use minor currency units. */
public record TaxBill(String id, TaxSubject subject, String currencyId, long amount,
                      long paidAmount, long createdAt, long dueAt, long graceUntil,
                      String sourceEventId, long periodStart, long periodEnd,
                      long taxableBase, int rateBasisPoints, long declarationDueAt,
                      long declaredAt, String declaredBy, long lateFeeAmount,
                      long lateFeeUpdatedAt) {
    public TaxBill {
        sourceEventId = sourceEventId == null ? "" : sourceEventId;
        taxableBase = Math.max(0L, taxableBase);
        rateBasisPoints = Math.max(0, rateBasisPoints);
        declaredBy = declaredBy == null ? "" : declaredBy;
        lateFeeAmount = Math.max(0L, lateFeeAmount);
    }

    public TaxBill(String id, TaxSubject subject, String currencyId, long amount,
                   long paidAmount, long createdAt, long dueAt, long graceUntil) {
        this(id, subject, currencyId, amount, paidAmount, createdAt, dueAt, graceUntil,
                "", 0L, 0L, 0L, 0, 0L, createdAt, "system", 0L, 0L);
    }

    public TaxBill(String id, TaxSubject subject, String currencyId, long amount,
                   long paidAmount, long createdAt, long dueAt, long graceUntil,
                   String sourceEventId, long periodStart, long periodEnd,
                   long taxableBase, int rateBasisPoints) {
        this(id, subject, currencyId, amount, paidAmount, createdAt, dueAt, graceUntil,
                sourceEventId, periodStart, periodEnd, taxableBase, rateBasisPoints,
                0L, createdAt, "system", 0L, 0L);
    }

    public boolean declared() { return declaredAt > 0L; }
    public long totalDue() { return addSaturated(amount, lateFeeAmount); }
    public long outstanding() { return Math.max(0L, totalDue() - paidAmount); }
    public boolean paid() { return outstanding() == 0L; }

    public long declarationDaysLate(long now) {
        if (declared() || declarationDueAt <= 0L || now <= declarationDueAt) return 0L;
        return Math.max(1L, (now - declarationDueAt) / 24000L);
    }

    public Status status(long now) {
        if (paid()) return Status.PAID;
        if (!declared()) return declarationDueAt > 0L && now > declarationDueAt
                ? Status.DECLARATION_OVERDUE : Status.DECLARATION_REQUIRED;
        if (dueAt > 0L && now > dueAt) {
            return graceUntil > 0L && now > graceUntil ? Status.DELINQUENT : Status.OVERDUE;
        }
        return Status.OPEN;
    }

    public TaxBill withPayment(long payment) {
        long next = paidAmount > Long.MAX_VALUE - payment ? Long.MAX_VALUE : paidAmount + payment;
        return new TaxBill(id, subject, currencyId, amount, Math.min(totalDue(), next), createdAt, dueAt, graceUntil,
                sourceEventId, periodStart, periodEnd, taxableBase, rateBasisPoints,
                declarationDueAt, declaredAt, declaredBy, lateFeeAmount, lateFeeUpdatedAt);
    }

    public TaxBill withAccrual(long additionalAmount, long newDueAt, long newGraceUntil) {
        long nextAmount = amount > Long.MAX_VALUE - additionalAmount
                ? Long.MAX_VALUE : amount + additionalAmount;
        return new TaxBill(id, subject, currencyId, nextAmount, paidAmount, createdAt,
                newDueAt, newGraceUntil, sourceEventId, periodStart, periodEnd, taxableBase, rateBasisPoints,
                declarationDueAt, declaredAt, declaredBy, lateFeeAmount, lateFeeUpdatedAt);
    }

    public TaxBill withDeclaration(long now, String playerId) {
        return new TaxBill(id, subject, currencyId, amount, paidAmount, createdAt, dueAt, graceUntil,
                sourceEventId, periodStart, periodEnd, taxableBase, rateBasisPoints,
                declarationDueAt, now, playerId, lateFeeAmount, lateFeeUpdatedAt);
    }

    public TaxBill withLateFee(long fee, long updatedAt) {
        return new TaxBill(id, subject, currencyId, amount, Math.min(totalDue(), paidAmount), createdAt, dueAt, graceUntil,
                sourceEventId, periodStart, periodEnd, taxableBase, rateBasisPoints,
                declarationDueAt, declaredAt, declaredBy, Math.max(0L, fee), updatedAt);
    }

    private static long addSaturated(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    public enum Status { DECLARATION_REQUIRED, DECLARATION_OVERDUE, OPEN, OVERDUE, DELINQUENT, FROZEN, PAID }
}
