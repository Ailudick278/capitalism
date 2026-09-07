package com.ailudick.capitalismmod.government;

import com.ailudick.capitalismmod.tax.TaxBill;
import com.ailudick.capitalismmod.tax.TaxPayment;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxRevenueAuditRulesTest {
    @Test
    void revenueMustMatchItsPaymentAndBill() {
        UUID taxpayer = UUID.randomUUID();
        TaxBill bill = new TaxBill("bill-1", new TaxSubject(TaxType.VAT, "subject", taxpayer),
                "usd", 1300L, 0L, 10L, 20L, 30L);
        TaxPayment payment = new TaxPayment("payment-1", bill.id(), taxpayer, "usd", 1300L, 20L);
        GovernmentPolicySavedData.TaxRevenue revenue = new GovernmentPolicySavedData.TaxRevenue(
                "tax-payment:payment-1", 1L, "subject", "VAT", "usd", 1300L, 1300L, 1300L);
        assertTrue(TaxRevenueAuditRules.matches(revenue, payment, bill, 1300L));
        assertFalse(TaxRevenueAuditRules.matches(revenue, payment, bill, 1200L));
    }
}
