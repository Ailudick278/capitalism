package com.ailudick.capitalismmod.loan;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PeerLoanTest {
    @Test
    void calculatesRegularAndOverdueInterest() {
        PeerLoan loan = new PeerLoan("id", UUID.randomUUID(), UUID.randomUUID(), "usd",
                3650, 0.10, 365, 0);
        assertEquals(365, loan.interestDue());
        assertEquals(730, loan.withDaysRemaining(-1).interestDue());
    }

    @Test
    void rejectsMalformedFinancialValues() {
        PeerLoan loan = new PeerLoan("id", UUID.randomUUID(), UUID.randomUUID(), "usd",
                -1, Double.NaN, 365, 1);
        assertEquals(0, loan.interestDue());
    }
}
