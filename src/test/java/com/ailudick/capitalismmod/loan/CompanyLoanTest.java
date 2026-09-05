package com.ailudick.capitalismmod.loan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompanyLoanTest {
    @Test
    void calculatesRegularAndOverdueInterest() {
        CompanyLoan loan = new CompanyLoan("id", "company", "usd", 3650,
                0.10, 365, 0, 0);
        assertEquals(365, loan.interestDue());
        assertEquals(732, loan.withDaysRemaining(-1).interestDue());
    }

    @Test
    void subtractsInterestAlreadyPaid() {
        CompanyLoan loan = new CompanyLoan("id", "company", "usd", 3650,
                0.10, 365, 0, 100);
        assertEquals(265, loan.interestDue());
    }

    @Test
    void supportsReducingOutstandingPrincipal() {
        CompanyLoan loan = new CompanyLoan("id", "company", "usd", 3650,
                0.10, 365, 0, 0);
        CompanyLoan reduced = loan.withPrincipal(2000);

        assertEquals(2000, reduced.principal());
        assertEquals(200, reduced.interestDue());
    }

    @Test
    void calculatesEqualPaymentForRemainingTerm() {
        CompanyLoan loan = new CompanyLoan("id", "company", "usd", 3650,
                0.10, 365, 365, 0);

        assertEquals(10, loan.scheduledPayment());
    }

    @Test
    void overdueLoanIsImmediatelyDue() {
        CompanyLoan loan = new CompanyLoan("id", "company", "usd", 3650,
                0.10, 365, -1, 0);

        assertEquals(4382, loan.scheduledPayment());
    }
}
