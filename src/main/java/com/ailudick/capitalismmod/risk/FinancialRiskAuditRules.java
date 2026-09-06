package com.ailudick.capitalismmod.risk;

/** Pure invariants for the normalized daily financial-risk snapshot. */
public final class FinancialRiskAuditRules {
    private FinancialRiskAuditRules() {
    }

    public static boolean validDerivedSnapshot(FinancialRiskSnapshot snapshot) {
        if (snapshot == null || snapshot.day() < 0L || snapshot.companyDebtMinor() < 0L
                || snapshot.peerDebtMinor() < 0L || snapshot.bankDebtMinor() < 0L
                || snapshot.bondLiabilityMinor() < 0L || snapshot.overdueDebtMinor() < 0L
                || snapshot.overdueLoanCount() < 0 || snapshot.overdueShareBasisPoints() < 0
                || snapshot.overdueShareBasisPoints() > 10000
                || snapshot.overdueDebtMinor() > snapshot.totalDebtMinor()) return false;
        long total = snapshot.totalDebtMinor();
        int expected = total <= 0L ? 0 : (int) Math.min(10000L,
                snapshot.overdueDebtMinor() > Long.MAX_VALUE / 10000L
                        ? 10000L : snapshot.overdueDebtMinor() * 10000L / total);
        return expected == snapshot.overdueShareBasisPoints();
    }
}
