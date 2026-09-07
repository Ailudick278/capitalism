package com.ailudick.capitalismmod.tax;

import java.util.UUID;

/** Pure invariants for persisted tax payment records. */
public final class TaxPaymentAuditRules {
    private TaxPaymentAuditRules() {}

    public static boolean valid(String id, String billId, UUID taxpayerUuid, String currencyId,
                                long amount, long paidAt, boolean currencyExists) {
        return id != null && !id.isBlank()
                && billId != null && !billId.isBlank()
                && taxpayerUuid != null
                && currencyId != null && !currencyId.isBlank() && currencyExists
                && amount > 0L && paidAt >= 0L;
    }

    public static boolean matchesBill(TaxPayment payment, TaxBill bill) {
        return payment != null && bill != null
                && payment.billId().equals(bill.id())
                && payment.taxpayerUuid().equals(bill.subject().taxpayerUuid())
                && payment.currencyId().equals(bill.currencyId());
    }

    public static boolean totalWithinDue(long totalPaid, long totalDue) {
        return totalPaid >= 0L && totalDue >= 0L && totalPaid <= totalDue;
    }

    public static boolean reconciles(long recordedPayments, long billPaidAmount) {
        return recordedPayments >= 0L && billPaidAmount >= 0L
                && recordedPayments == billPaidAmount;
    }
}
