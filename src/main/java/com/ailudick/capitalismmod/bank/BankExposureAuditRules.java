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
}
