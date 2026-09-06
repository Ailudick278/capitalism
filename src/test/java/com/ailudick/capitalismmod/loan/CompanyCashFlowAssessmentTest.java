package com.ailudick.capitalismmod.loan;

import com.ailudick.capitalismmod.company.CompanyLedgerEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyCashFlowAssessmentTest {
    @Test
    void financingFlowsDoNotPretendToBeOperatingCashFlow() {
        CompanyCashFlowAssessment assessment = CompanyCashFlowAssessment.evaluate(List.of(
                entry("loan_proceeds", 100_000L, 100L),
                entry("capital_contribution", 100_000L, 100L),
                entry("dividend_distribution", -100_000L, 100L)),
                100L, 1_000L, 0L, 1_000L);

        assertFalse(assessment.hasOperatingHistory());
        assertTrue(assessment.approved());
    }

    @Test
    void negativeOperatingCashFlowBlocksNewDebt() {
        CompanyCashFlowAssessment assessment = CompanyCashFlowAssessment.evaluate(List.of(
                entry("revenue", 100L, 100L),
                entry("production_expense", -150L, 100L)),
                100L, 1_000L, 0L, 1L);

        assertTrue(assessment.hasOperatingHistory());
        assertTrue(assessment.operatingCashFlow() < 0L);
        assertFalse(assessment.approved());
    }

    @Test
    void profitableOperationsSupportDebtWithinCashFlowMultiple() {
        CompanyCashFlowAssessment assessment = CompanyCashFlowAssessment.evaluate(List.of(
                entry("revenue", 1_000L, 100L),
                entry("production_expense", -100L, 100L)),
                100L, 1_000L, 500L, 2_000L);

        assertTrue(assessment.approved());
        assertTrue(assessment.maximumSupportedDebt() >= 2_000L);
    }

    @Test
    void nonCashOperatingEntriesDoNotReduceLendingCashFlow() {
        CompanyCashFlowAssessment assessment = CompanyCashFlowAssessment.evaluate(List.of(
                entry("revenue", 1_000L, 100L),
                entry("cost_of_goods_sold", -700L, 100L),
                entry("accrued_expense", -200L, 100L),
                entry("inventory_loss", -100L, 100L)),
                100L, 1_000L, 0L, 2_000L);

        assertTrue(assessment.hasOperatingHistory());
        assertTrue(assessment.operatingCashFlow() == 1_000L);
        assertTrue(assessment.approved());
    }

    @Test
    void cashFlowDebtMultipleIsPolicyControlled() {
        CompanyCashFlowAssessment conservative = CompanyCashFlowAssessment.evaluate(List.of(
                entry("revenue", 1_000L, 100L)), 100L, 1_000L, 0L, 2_500L, 2.0);
        CompanyCashFlowAssessment permissive = CompanyCashFlowAssessment.evaluate(List.of(
                entry("revenue", 1_000L, 100L)), 100L, 1_000L, 0L, 2_500L, 3.0);

        assertFalse(conservative.approved());
        assertTrue(permissive.approved());
    }

    private static CompanyLedgerEntry entry(String type, long amount, long timestamp) {
        return new CompanyLedgerEntry("company", timestamp, type, "usd", amount, 0L, type);
    }
}
