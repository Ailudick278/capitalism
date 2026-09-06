package com.ailudick.capitalismmod.loan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyLoanPaymentAllocationTest {
    @Test
    void partialPaymentClearsInterestBeforePrincipal() {
        CompanyLoan loan = new CompanyLoan("loan", "company", "usd", 1000L,
                1.0, 365, 0, 0L, -1L);

        var allocation = CompanyLoanPaymentAllocation.forAmount(loan, 1050L).orElseThrow();

        assertEquals(1000L, allocation.interestPayment());
        assertEquals(50L, allocation.principalPayment());
        assertEquals(950L, allocation.remainingPrincipal());
    }

    @Test
    void fullPaymentLeavesNoPrincipal() {
        CompanyLoan loan = new CompanyLoan("loan", "company", "usd", 1000L,
                1.0, 365, 0, 0L, -1L);

        var allocation = CompanyLoanPaymentAllocation.forAmount(loan, 2000L).orElseThrow();

        assertEquals(1000L, allocation.interestPayment());
        assertEquals(1000L, allocation.principalPayment());
        assertEquals(0L, allocation.remainingPrincipal());
    }

    @Test
    void paymentAboveBalanceIsRejected() {
        CompanyLoan loan = new CompanyLoan("loan", "company", "usd", 1000L,
                1.0, 365, 0, 0L, -1L);

        assertTrue(CompanyLoanPaymentAllocation.forAmount(loan, 2001L).isEmpty());
    }
}
