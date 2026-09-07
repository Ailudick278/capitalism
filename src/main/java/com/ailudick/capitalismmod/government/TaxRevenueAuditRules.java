package com.ailudick.capitalismmod.government;

import com.ailudick.capitalismmod.tax.TaxBill;
import com.ailudick.capitalismmod.tax.TaxPayment;

/** Pure linkage rules between a fiscal tax-revenue receipt and its payment. */
public final class TaxRevenueAuditRules {
    private TaxRevenueAuditRules() {}

    public static boolean validId(String revenueId, String paymentId) {
        return paymentId != null && !paymentId.isBlank()
                && revenueId != null && revenueId.equals("tax-payment:" + paymentId);
    }

    public static boolean matches(GovernmentPolicySavedData.TaxRevenue revenue,
                                  TaxPayment payment, TaxBill bill, long expectedConverted) {
        return revenue != null && payment != null && bill != null
                && validId(revenue.id(), payment.id())
                && revenue.subject().equals(bill.subject().subjectId())
                && revenue.taxType().equals(bill.subject().type().name())
                && revenue.currencyId().equals(payment.currencyId())
                && revenue.originalAmount() == payment.amount()
                && revenue.convertedAmount() == expectedConverted
                && revenue.day() >= 0L
                && revenue.balanceAfter() >= 0L;
    }
}
