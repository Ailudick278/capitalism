package com.ailudick.capitalismmod.loan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyDebtServiceAssessmentTest {
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
}
