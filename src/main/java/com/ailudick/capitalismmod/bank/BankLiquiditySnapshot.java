package com.ailudick.capitalismmod.bank;

/** Daily normalized view of player-bank deposits and credit balances. */
public record BankLiquiditySnapshot(long day, long depositsMinor, long loanDebtMinor,
                                    long withdrawnMinor, boolean withdrawalLimitActive) {
    public long totalExposureMinor() {
        return depositsMinor > Long.MAX_VALUE - loanDebtMinor ? Long.MAX_VALUE : depositsMinor + loanDebtMinor;
    }
}
