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
}
