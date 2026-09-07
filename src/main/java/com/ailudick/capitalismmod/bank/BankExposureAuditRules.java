package com.ailudick.capitalismmod.bank;

/** Pure comparison rules for authoritative account exposure and its persisted snapshot. */
public final class BankExposureAuditRules {
    private BankExposureAuditRules() {
    }

    public static boolean matches(BankExposureSavedData.Exposure expected,
                                  BankExposureSavedData.Exposure actual) {
        return expected != null && actual != null
                && expected.depositsMinor() == actual.depositsMinor()
                && expected.loanDebtMinor() == actual.loanDebtMinor()
                && expected.overdueDebtMinor() == actual.overdueDebtMinor()
                && expected.overdueAccounts() == actual.overdueAccounts();
    }

    public static boolean validAccountSnapshot(BankExposureSavedData.AccountSnapshot snapshot) {
        return snapshot != null && snapshot.accountId() != null && !snapshot.accountId().isBlank()
                && snapshot.depositsMinor() >= 0L && snapshot.loanDebtMinor() >= 0L
                && snapshot.overdueDebtMinor() >= 0L && snapshot.overdueDebtMinor() <= snapshot.loanDebtMinor()
                && snapshot.syncedAt() >= 0L;
    }

    public static boolean totalsMatch(BankExposureSavedData.Exposure exposure,
                                      long deposits, long loans, long overdue, int overdueAccounts) {
        return exposure != null && deposits == exposure.depositsMinor() && loans == exposure.loanDebtMinor()
                && overdue == exposure.overdueDebtMinor() && overdueAccounts == exposure.overdueAccounts();
    }

    public static boolean timestampMatches(BankExposureSavedData.Exposure exposure,
                                           BankExposureSavedData.AccountSnapshot snapshot) {
        return exposure != null && validAccountSnapshot(snapshot)
                && exposure.syncedAt() == snapshot.syncedAt();
    }

    public static boolean transactionSummaryMatches(BankExposureSavedData.AccountSnapshot snapshot,
                                                     int transactionCount, long lastTransactionAt) {
        return validAccountSnapshot(snapshot) && transactionCount >= 0
                && snapshot.transactionCount() == transactionCount
                && snapshot.lastTransactionAt() == lastTransactionAt;
    }
}
