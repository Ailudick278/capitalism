package com.ailudick.capitalismmod.bank;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankRecoveryRulesTest {
    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void validatesCashDepositPrecondition() {
        assertTrue(BankRecoveryRules.validCashDeposit("deposit-1", PLAYER, "account-1", "usd",
                50L, 100L, true));
        assertFalse(BankRecoveryRules.validCashDeposit("deposit-1", PLAYER, "account-1", "usd",
                150L, 100L, true));
        assertFalse(BankRecoveryRules.validCashDeposit("deposit-1", PLAYER, "account-1", "bad",
                50L, 100L, false));
    }

    @Test
    void validatesDebtRepaymentPrecondition() {
        assertTrue(BankRecoveryRules.validRepayment("repay-1", PLAYER, "account-1", "usd",
                100L, 50L, true));
        assertFalse(BankRecoveryRules.validRepayment("repay-1", PLAYER, "account-1", "usd",
                40L, 50L, true));
        assertFalse(BankRecoveryRules.validRepayment("repay-1", PLAYER, "account-1", "bad",
                100L, 50L, false));
    }
}
