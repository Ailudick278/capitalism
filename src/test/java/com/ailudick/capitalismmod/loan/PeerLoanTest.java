package com.ailudick.capitalismmod.loan;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeerLoanTest {
    @Test
    void calculatesRegularAndOverdueInterest() {
        PeerLoan loan = new PeerLoan("id", UUID.randomUUID(), UUID.randomUUID(), "usd",
                3650, 0.10, 365, 0);
        assertEquals(365, loan.interestDue());
        assertEquals(367, loan.withDaysRemaining(-1).interestDue());
    }

    @Test
    void rejectsMalformedFinancialValues() {
        PeerLoan loan = new PeerLoan("id", UUID.randomUUID(), UUID.randomUUID(), "usd",
                -1, Double.NaN, 365, 1);
        assertEquals(0, loan.interestDue());
    }

    @Test
    void subtractsInterestAlreadyPaid() {
        PeerLoan loan = new PeerLoan("id", UUID.randomUUID(), UUID.randomUUID(), "usd",
                3650, 0.10, 365, 0).withInterestPaid(100);
        assertEquals(265, loan.interestDue());
    }

    @Test
    void partialPrincipalPaymentPreservesInterestAccruedBeforePayment() {
        PeerLoan loan = new PeerLoan("id", UUID.randomUUID(), UUID.randomUUID(), "usd",
                3650, 0.10, 365, 265);
        long accruedBeforePayment = loan.totalInterestAccrued();
        PeerLoan reduced = loan.withInterestPaid(accruedBeforePayment)
                .withPrincipal(1825)
                .withInterestAccrualState(accruedBeforePayment, 100L);

        assertEquals(0L, reduced.interestDue());
        PeerLoan threeDaysLater = reduced.withDaysRemaining(262);
        assertTrue(threeDaysLater.interestDue() > 0L);
        assertTrue(threeDaysLater.interestDue() < loan.interestDue() + 2L);
    }
}
