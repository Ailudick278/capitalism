package com.ailudick.capitalismmod.loan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyDebtServiceAssessmentTest {
    @Test
    void overdueExistingLoanBlocksAutomatedNewCredit() {
        CompanyLoan overdue = new CompanyLoan("id", "company", "usd", 1_000L,
                0.10, 30, -1, 0L);
        CompanyLoan current = new CompanyLoan("id2", "company", "usd", 1_000L,
                0.10, 30, 1, 0L);

        assertTrue(CompanyDebtServiceAssessment.hasOverdueLoan(List.of(overdue, current)));
        assertFalse(CompanyDebtServiceAssessment.hasOverdueLoan(List.of(current)));
    }

    @Test
    void shortTermLoanRequiresMoreCashFlowThanLongTermLoan() {
        CompanyDebtServiceAssessment shortTerm = CompanyDebtServiceAssessment.evaluate(
                1_000L, List.of(), 2_000L, 30, 0.10, true);
        CompanyDebtServiceAssessment longTerm = CompanyDebtServiceAssessment.evaluate(
                1_000L, List.of(), 2_000L, 3650, 0.10, true);

        assertFalse(shortTerm.approved());
        assertTrue(longTerm.approved());
        assertTrue(shortTerm.annualDebtService() > longTerm.annualDebtService());
    }

    @Test
    void startupCompanyWithoutHistoryKeepsStartupPath() {
        CompanyDebtServiceAssessment assessment = CompanyDebtServiceAssessment.evaluate(
                0L, List.of(), 100_000L, 30, 0.20, false);

        assertTrue(assessment.approved());
    }

    @Test
    void configurableCoverageRatioCanTightenApproval() {
        CompanyDebtServiceAssessment standard = CompanyDebtServiceAssessment.evaluate(
                1_000L, List.of(), 2_000L, 3650, 0.10, true, 1.25);
        CompanyDebtServiceAssessment stricter = CompanyDebtServiceAssessment.evaluate(
                1_000L, List.of(), 2_000L, 3650, 0.10, true, 12.00);

        assertTrue(standard.approved());
        assertFalse(stricter.approved());
        assertTrue(stricter.coverageRatio() < 12.00);
    }
}
