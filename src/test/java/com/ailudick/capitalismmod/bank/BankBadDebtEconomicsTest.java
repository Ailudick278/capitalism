package com.ailudick.capitalismmod.bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankBadDebtEconomicsTest {
    @Test
    void onlyLongOverdueLoansAreEligible() {
        assertFalse(BankBadDebtEconomics.eligible(-89, 90));
        assertTrue(BankBadDebtEconomics.eligible(-90, 90));
        assertTrue(BankBadDebtEconomics.eligible(-120, 90));
        assertFalse(BankBadDebtEconomics.eligible(-120, 0));
    }
}
