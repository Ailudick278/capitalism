package com.ailudick.capitalismmod.bank;

import java.util.Set;

/** Pure invariants for bank account snapshots and transaction history. */
public final class BankTransactionAuditRules {
    private static final Set<String> TYPES = Set.of("deposit", "withdraw", "loan", "repay", "interest",
            "term_deposit", "term_withdraw", "term_maturity", "exchange", "transfer_in", "transfer_out",
            "transfer_fee", "bad_debt_writeoff");

    private BankTransactionAuditRules() {}

    public static boolean validAccount(String id, boolean credit, java.util.Map<String, Long> balances,
                                       java.util.Map<String, Long> debts, int loanDaysRemaining) {
        if (id == null || id.isBlank() || balances == null || debts == null) return false;
        return balances.entrySet().stream().allMatch(e -> e.getKey() != null && !e.getKey().isBlank()
                && e.getValue() != null && e.getValue() >= 0L)
                && debts.entrySet().stream().allMatch(e -> e.getKey() != null && !e.getKey().isBlank()
                && e.getValue() != null && e.getValue() >= 0L);
    }

    public static boolean validTransaction(String type, String currencyId, long amount, long occurredAt,
                                           String reference, boolean currencyExists) {
        if (!TYPES.contains(type) || currencyId == null || currencyId.isBlank() || !currencyExists
                || amount == 0L || occurredAt < -1L || reference == null) return false;
        if (amount > 0L) return !Set.of("withdraw", "repay", "interest", "term_deposit", "transfer_out",
                "transfer_fee", "bad_debt_writeoff").contains(type);
        return Set.of("withdraw", "repay", "interest", "term_deposit", "transfer_out", "transfer_fee",
                "bad_debt_writeoff").contains(type);
    }
}
