package com.ailudick.capitalismmod.bank;

/** Pure liquidity-policy arithmetic. */
public final class BankLiquidityEconomics {
    private BankLiquidityEconomics() {}

    /** Crisis-period daily withdrawal capacity: 10% of observed deposits. */
    public static long crisisWithdrawalLimit(long depositsMinor) {
        return depositsMinor <= 0L ? 0L : depositsMinor / 10L;
    }

    /** Crisis-period aggregate credit ceiling: 80% loan-to-deposit ratio. */
    public static long crisisLoanCapacity(long depositsMinor) {
        return depositsMinor <= 0L ? 0L : depositsMinor / 5L * 4L;
    }
}
