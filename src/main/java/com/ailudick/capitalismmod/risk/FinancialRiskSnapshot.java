package com.ailudick.capitalismmod.risk;

/** One daily, base-currency-normalized view of financial-sector liabilities. */
public record FinancialRiskSnapshot(long day, long companyDebtMinor, long peerDebtMinor,
                                    long bondLiabilityMinor, long overdueDebtMinor,
                                    int overdueLoanCount, int overdueShareBasisPoints) {
    public long totalDebtMinor() {
        return add(add(companyDebtMinor, peerDebtMinor), bondLiabilityMinor);
    }

    private static long add(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }
}
