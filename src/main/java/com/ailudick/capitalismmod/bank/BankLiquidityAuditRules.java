package com.ailudick.capitalismmod.bank;

import java.util.List;

/** Pure invariants for the persisted bank-liquidity history. */
public final class BankLiquidityAuditRules {
    private BankLiquidityAuditRules() {
    }

    public static boolean validHistory(List<BankLiquiditySnapshot> snapshots) {
        if (snapshots == null) return false;
        long previousDay = -1L;
        for (BankLiquiditySnapshot snapshot : snapshots) {
            if (snapshot == null || snapshot.day() < 0L || snapshot.day() <= previousDay
                    || snapshot.depositsMinor() < 0L || snapshot.loanDebtMinor() < 0L
                    || snapshot.withdrawnMinor() < 0L || snapshot.emergencyLiquidityMinor() < 0L
                    || snapshot.withdrawnMinor() > snapshot.depositsMinor()
                    && snapshot.withdrawnMinor() - snapshot.depositsMinor() > snapshot.emergencyLiquidityMinor()) {
                return false;
            }
            previousDay = snapshot.day();
        }
        return true;
    }
}
