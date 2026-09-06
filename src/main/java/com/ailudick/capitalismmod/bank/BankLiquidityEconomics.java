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

    /** Returns whether bank loans exceed 120% of deposits, a severe balance-sheet stress signal. */
    public static boolean solvencyStress(long depositsMinor, long loansMinor) {
        if (loansMinor <= 0L) return false;
        if (depositsMinor <= 0L) return true;
        return loansMinor > depositsMinor + depositsMinor / 5L;
    }

    /** Returns whether same-day withdrawals are large enough to indicate a run. */
    public static boolean withdrawalRunStress(long depositsMinor, long withdrawnMinor) {
        return depositsMinor > 0L && withdrawnMinor > depositsMinor / 2L;
    }
}
