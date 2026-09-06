package com.ailudick.capitalismmod.bank;

/** Conservative rule for converting a long-overdue bank loan into a realized loss. */
public final class BankBadDebtEconomics {
    private BankBadDebtEconomics() {}

    public static boolean eligible(int loanDaysRemaining, int writeOffAfterDays) {
        return writeOffAfterDays > 0 && loanDaysRemaining <= -writeOffAfterDays;
    }
}
