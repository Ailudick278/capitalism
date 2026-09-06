package com.ailudick.capitalismmod.economy;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryIntentRulesTest {
    private static final UUID LENDER = UUID.randomUUID();
    private static final UUID BORROWER = UUID.randomUUID();

    @Test
    void validatesRecoveryParticipantsAndCurrencies() {
        assertTrue(RecoveryIntentRules.validExchange("fx-1", LENDER, "usd", "cny",
                100L, 700L, true, true));
        assertTrue(RecoveryIntentRules.validTransfer("transfer-1", LENDER, BORROWER,
                "usd", 100L, true));
        assertFalse(RecoveryIntentRules.validTransfer("transfer-1", LENDER, LENDER,
                "usd", 100L, true));
        assertFalse(RecoveryIntentRules.validExchange("fx-1", LENDER, "usd", "usd",
                100L, 100L, true, true));
    }

    @Test
    void validatesLoanTermsAndPaymentAllocation() {
        assertTrue(RecoveryIntentRules.validOrigination("loan-1", LENDER, BORROWER,
                "usd", 1_000L, 0.05, 30, true));
        assertTrue(RecoveryIntentRules.validPayment("loan-1", LENDER, BORROWER, "usd",
                10L, 110L, 10L, 100L, 900L, 20, true));
        assertFalse(RecoveryIntentRules.validOrigination("loan-1", LENDER, BORROWER,
                "usd", 1_000L, Double.NaN, 30, true));
        assertFalse(RecoveryIntentRules.validPayment("loan-1", LENDER, BORROWER, "usd",
                10L, 110L, 11L, 100L, 900L, 20, true));
    }
}
