package com.ailudick.capitalismmod.bank;

/** Pure liquidity-policy arithmetic. */
public final class BankLiquidityEconomics {
    private BankLiquidityEconomics() {}

    /** Crisis-period daily withdrawal capacity: 10% of observed deposits. */
    public static long crisisWithdrawalLimit(long depositsMinor) {
        return depositsMinor <= 0L ? 0L : depositsMinor / 10L;
    }
}
