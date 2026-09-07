package com.ailudick.capitalismmod.bank;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditAssessmentTest {
    @Test
    void unusedAccountReceivesNormalStarterLimit() {
        CreditAssessment assessment = CreditAssessment.evaluate(
                new BankAccount("1", true, Map.of(), Map.of(), List.of(), List.of(), 0), 0, 100_000);
        assertEquals(700, assessment.score());
        assertEquals(82_352, assessment.approvedLimit());
    }

    @Test
    void repaymentHistoryImprovesScore() {
        BankAccount account = new BankAccount("1", true, Map.of(), Map.of(),
                List.of(new BankTransaction("repay", "usd", -100L)), List.of(), 0);
        assertTrue(CreditAssessment.evaluate(account, 0, 100_000).score() > 700);
    }

    @Test
    void staleExposureHalvesApprovedLimit() {
        BankAccount account = new BankAccount("1", true, Map.of(), Map.of(), List.of(), List.of(), 0);
        long normal = CreditAssessment.evaluate(account, 0, 100_000).approvedLimit();
        long stale = CreditAssessment.evaluate(account, 0, 100_000, true).approvedLimit();
        assertEquals(normal / 2L, stale);
    }

    @Test
    void overdueDebtReceivesSeverePenalty() {
        BankAccount account = new BankAccount("1", true, Map.of(), Map.of("usd", 100L),
                List.of(), List.of(), -1);
        CreditAssessment assessment = CreditAssessment.evaluate(account, 50_000, 100_000);
        assertTrue(assessment.overdue());
        assertTrue(assessment.score() <= 450);
    }
}
