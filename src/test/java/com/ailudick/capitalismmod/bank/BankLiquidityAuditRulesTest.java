package com.ailudick.capitalismmod.bank;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankLiquidityAuditRulesTest {
    @Test
    void requiresStrictlyIncreasingSettlementDays() {
        assertTrue(BankLiquidityAuditRules.validHistory(List.of(
                new BankLiquiditySnapshot(1L, 100L, 80L, 5L, true),
                new BankLiquiditySnapshot(2L, 110L, 80L, 6L, false))));
        assertFalse(BankLiquidityAuditRules.validHistory(List.of(
                new BankLiquiditySnapshot(1L, 100L, 80L, 5L, true),
                new BankLiquiditySnapshot(1L, 110L, 80L, 6L, false))));
        assertFalse(BankLiquidityAuditRules.validHistory(List.of(
                new BankLiquiditySnapshot(1L, 100L, 80L, 101L, true))));
        assertTrue(BankLiquidityAuditRules.validHistory(List.of(
                new BankLiquiditySnapshot(1L, 100L, 80L, 110L, true, 10L))));
    }
}
