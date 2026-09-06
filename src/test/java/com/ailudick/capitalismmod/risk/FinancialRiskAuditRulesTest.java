package com.ailudick.capitalismmod.risk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialRiskAuditRulesTest {
    @Test
    void validatesDerivedOverdueShare() {
        var valid = new FinancialRiskSnapshot(1L, 100L, 100L, 200L, 100L,
                50L, 1, 1000);
        var invalid = new FinancialRiskSnapshot(1L, 100L, 100L, 200L, 100L,
                50L, 1, 1001);
        assertTrue(FinancialRiskAuditRules.validDerivedSnapshot(valid));
        assertFalse(FinancialRiskAuditRules.validDerivedSnapshot(invalid));
    }
}
