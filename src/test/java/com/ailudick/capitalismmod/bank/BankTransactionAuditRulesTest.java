package com.ailudick.capitalismmod.bank;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankTransactionAuditRulesTest {
    @Test
    void accountBalancesAndDebtsMustBeNonNegative() {
        assertTrue(BankTransactionAuditRules.validAccount("a", true, Map.of("usd", 1L), Map.of("usd", 2L), 3));
        assertFalse(BankTransactionAuditRules.validAccount("a", true, Map.of("usd", -1L), Map.of(), 3));
    }

    @Test
    void transactionDirectionMustFitItsType() {
        assertTrue(BankTransactionAuditRules.validTransaction("deposit", "usd", 10L, 1L, "ref", true));
        assertTrue(BankTransactionAuditRules.validTransaction("repay", "usd", -10L, 1L, "ref", true));
        assertFalse(BankTransactionAuditRules.validTransaction("repay", "usd", 10L, 1L, "ref", true));
    }
}
