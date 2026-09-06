package com.ailudick.capitalismmod.bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankCapitalAuditRulesTest {
    @Test
    void keepsUninitializedLedgerEmpty() {
        assertTrue(BankCapitalAuditRules.validState(false, 0L, 0L, 0L, -1L));
        assertFalse(BankCapitalAuditRules.validState(false, 1L, 0L, 0L, -1L));
        assertFalse(BankCapitalAuditRules.validState(true, Long.MIN_VALUE, 0L, 0L, 0L));
    }

    @Test
    void checksProvisionOnlyAfterTheBankClose() {
        assertTrue(BankCapitalAuditRules.provisionMatchesAfterClose(0L, 100L, 4L, 5L));
        assertTrue(BankCapitalAuditRules.provisionMatchesAfterClose(50L, 100L, 5L, 5L));
        assertFalse(BankCapitalAuditRules.provisionMatchesAfterClose(49L, 100L, 5L, 5L));
    }
}
