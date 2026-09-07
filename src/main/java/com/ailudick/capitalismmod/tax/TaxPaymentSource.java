package com.ailudick.capitalismmod.tax;

/** Stable identifiers for replayable tax settlement operations. */
public final class TaxPaymentSource {
    private TaxPaymentSource() {}

    public static String company(String billId, long paidBeforeMinor, long majorAmount) {
        if (billId == null || billId.isBlank() || paidBeforeMinor < 0L || majorAmount <= 0L) return "";
        return "tax-company-payment:" + billId + ":" + paidBeforeMinor + ":" + majorAmount;
    }

    public static String external(String sourceId, String billId) {
        if (sourceId == null || sourceId.isBlank() || billId == null || billId.isBlank()) return "";
        return "tax-external-payment:" + sourceId + ":" + billId;
    }
}
