package com.ailudick.capitalismmod.tax;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxPaymentAuditRulesTest {
    @Test
    void paymentMustMatchBillIdentity() {
        UUID taxpayer = UUID.randomUUID();
        TaxBill bill = new TaxBill("bill-1", new TaxSubject(TaxType.VAT, "subject", taxpayer),
                "usd", 1100L, 0L, 10L, 20L, 30L);
        TaxPayment payment = new TaxPayment("payment-1", "bill-1", taxpayer, "usd", 1100L, 20L);
        assertTrue(TaxPaymentAuditRules.valid(payment.id(), payment.billId(), payment.taxpayerUuid(),
                payment.currencyId(), payment.amount(), payment.paidAt(), true));
        assertTrue(TaxPaymentAuditRules.matchesBill(payment, bill));
        assertFalse(TaxPaymentAuditRules.matchesBill(
                new TaxPayment("payment-2", "bill-1", UUID.randomUUID(), "usd", 1L, 20L), bill));
    }

    @Test
    void totalAndReconciliationAreBounded() {
        assertTrue(TaxPaymentAuditRules.totalWithinDue(100L, 100L));
        assertFalse(TaxPaymentAuditRules.totalWithinDue(101L, 100L));
        assertTrue(TaxPaymentAuditRules.reconciles(100L, 100L));
        assertFalse(TaxPaymentAuditRules.reconciles(99L, 100L));
    }
}
