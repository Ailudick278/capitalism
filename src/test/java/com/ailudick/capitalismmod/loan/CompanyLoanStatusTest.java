package com.ailudick.capitalismmod.loan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyLoanStatusTest {
    @Test
    void dueAndOverdueTransitionsAreDetectable() {
        CompanyLoan due = new CompanyLoan("id", "company", "usd", 1000L, 0.05, 30, 1, 0L);
        assertTrue(due.becomesDueAfter(0));
        assertFalse(due.becomesOverdueAfter(0));
        assertFalse(due.isOverdue());

        CompanyLoan overdue = due.withDaysRemaining(0);
        assertTrue(overdue.becomesOverdueAfter(-1));
        assertTrue(overdue.withDaysRemaining(-1).isOverdue());
    }
}
