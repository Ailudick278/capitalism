package com.ailudick.capitalismmod.bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BankLiquidityEconomicsTest {
    @Test
    void crisisWithdrawalCapacityIsTenPercentOfDeposits() {
        assertEquals(10_000L, BankLiquidityEconomics.crisisWithdrawalLimit(100_000L));
        assertEquals(0L, BankLiquidityEconomics.crisisWithdrawalLimit(0L));
        assertEquals(80_000L, BankLiquidityEconomics.crisisLoanCapacity(100_000L));
        assertEquals(80_000L, BankLiquidityEconomics.riskAdjustedLoanCapacity(100_000L, 0));
        assertEquals(60_000L, BankLiquidityEconomics.riskAdjustedLoanCapacity(100_000L, 5000));
        assertEquals(40_000L, BankLiquidityEconomics.riskAdjustedLoanCapacity(100_000L, 10000));
        assertEquals(73_600L, BankLiquidityEconomics.capitalAdjustedLoanCapacity(100_000L, 0));
        assertEquals(36_800L, BankLiquidityEconomics.capitalAdjustedLoanCapacity(100_000L, 10000));
        assertEquals(false, BankLiquidityEconomics.solvencyStress(100_000L, 120_000L));
        assertEquals(true, BankLiquidityEconomics.solvencyStress(100_000L, 120_001L));
        assertEquals(true, BankLiquidityEconomics.solvencyStress(0L, 1L));
        assertEquals(false, BankLiquidityEconomics.solvencyStress(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(false, BankLiquidityEconomics.withdrawalRunStress(100_000L, 50_000L));
        assertEquals(true, BankLiquidityEconomics.withdrawalRunStress(100_000L, 50_001L));
        assertEquals(12_500L, BankCapitalEconomics.capitalBackedLoanCapacity(1_000L));
        assertEquals(0L, BankCapitalEconomics.capitalBackedLoanCapacity(0L));
        assertEquals(true, BankCapitalEconomics.capitalStress(799L, 10_000L));
        assertEquals(false, BankCapitalEconomics.capitalStress(800L, 10_000L));
        assertEquals(501L, BankCapitalEconomics.lossProvisionTarget(1_001L));
        assertEquals(0L, BankCapitalEconomics.lossProvisionTarget(0L));
    }
}
