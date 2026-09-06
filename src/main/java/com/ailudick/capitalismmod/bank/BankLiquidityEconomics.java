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

    /** Crisis lending capacity after applying a 0-10000 basis-point delinquency discount. */
    public static long riskAdjustedLoanCapacity(long depositsMinor, int overdueShareBasisPoints) {
        long base = crisisLoanCapacity(depositsMinor);
        if (base <= 0L) return 0L;
        int share = Math.max(0, Math.min(10000, overdueShareBasisPoints));
        long scale = 10000L - share / 2L;
        return (base / 10000L) * scale + (base % 10000L) * scale / 10000L;
    }

    /**
     * Crisis lending capacity after reserving a minimum 8% capital buffer.
     * The buffer is applied to the already risk-adjusted capacity because this
     * version of the simulation has no separate bank-equity account yet.
     */
    public static long capitalAdjustedLoanCapacity(long depositsMinor, int overdueShareBasisPoints) {
        long riskAdjusted = riskAdjustedLoanCapacity(depositsMinor, overdueShareBasisPoints);
        return (riskAdjusted / 10000L) * 9200L + (riskAdjusted % 10000L) * 9200L / 10000L;
    }

    /** Returns whether bank loans exceed 120% of deposits, a severe balance-sheet stress signal. */
    public static boolean solvencyStress(long depositsMinor, long loansMinor) {
        if (loansMinor <= 0L) return false;
        if (depositsMinor <= 0L) return true;
        long threshold = depositsMinor / 5L > Long.MAX_VALUE - depositsMinor
                ? Long.MAX_VALUE : depositsMinor + depositsMinor / 5L;
        return loansMinor > threshold;
    }

    /** Returns whether same-day withdrawals are large enough to indicate a run. */
    public static boolean withdrawalRunStress(long depositsMinor, long withdrawnMinor) {
        return depositsMinor > 0L && withdrawnMinor > depositsMinor / 2L;
    }
}
