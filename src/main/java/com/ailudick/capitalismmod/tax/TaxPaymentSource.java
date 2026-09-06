package com.ailudick.capitalismmod.tax;

/** Stable identifiers for replayable tax settlement operations. */
public final class TaxPaymentSource {
    private TaxPaymentSource() {}

    public static String company(String billId, long paidBeforeMinor, long majorAmount) {
        if (billId == null || billId.isBlank() || paidBeforeMinor < 0L || majorAmount <= 0L) return "";
        return "tax-payment:" + billId + ":" + paidBeforeMinor + ":" + majorAmount;
    }
}
