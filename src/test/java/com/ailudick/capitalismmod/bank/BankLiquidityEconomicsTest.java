package com.ailudick.capitalismmod.bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BankLiquidityEconomicsTest {
    @Test
    void crisisWithdrawalCapacityIsTenPercentOfDeposits() {
        assertEquals(10_000L, BankLiquidityEconomics.crisisWithdrawalLimit(100_000L));
        assertEquals(0L, BankLiquidityEconomics.crisisWithdrawalLimit(0L));
        assertEquals(80_000L, BankLiquidityEconomics.crisisLoanCapacity(100_000L));
    }
}
