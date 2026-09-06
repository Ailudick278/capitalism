package com.ailudick.capitalismmod.loan;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PeerLoanPaymentAllocationTest {
    @Test
    void allocatesInterestBeforePrincipal() {
        PeerLoan loan = new PeerLoan("loan", UUID.randomUUID(), UUID.randomUUID(), "usd",
                1000L, 0.10, 365, 0);
        PeerLoanPaymentAllocation allocation = PeerLoanPaymentAllocation.forAmount(loan, 200L).orElseThrow();

        assertEquals(100L, allocation.interestPayment());
        assertEquals(100L, allocation.principalPayment());
        assertEquals(900L, allocation.remainingPrincipal());
    }
}
