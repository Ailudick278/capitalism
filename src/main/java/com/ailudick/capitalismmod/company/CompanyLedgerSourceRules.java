package com.ailudick.capitalismmod.company;

/** Pure identity checks for idempotent company-ledger source receipts. */
public final class CompanyLedgerSourceRules {
    private CompanyLedgerSourceRules() {}

    public static boolean matches(CompanyLedgerEntry entry, String currencyId, long amount, boolean credit) {
        if (entry == null || currencyId == null || currencyId.isBlank() || amount <= 0L) return false;
        long expected = credit ? amount : -amount;
        return currencyId.equals(entry.currencyId()) && entry.amount() == expected;
    }
}
