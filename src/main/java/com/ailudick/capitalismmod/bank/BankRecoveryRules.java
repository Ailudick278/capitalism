package com.ailudick.capitalismmod.bank;

import java.util.UUID;

/** Pure validation rules for crash-safe bank operation intents. */
public final class BankRecoveryRules {
    private BankRecoveryRules() {
    }

    public static boolean validCashDeposit(String source, UUID player, String account, String currency,
                                           long amount, long physicalBefore, boolean currencyExists) {
        return nonBlank(source) && player != null && nonBlank(account) && nonBlank(currency)
                && amount > 0L && physicalBefore >= amount && currencyExists;
    }

    public static boolean validRepayment(String id, UUID player, String account, String currency,
                                         long debtBefore, long amount, boolean currencyExists) {
        return nonBlank(id) && player != null && nonBlank(account) && nonBlank(currency)
                && debtBefore >= amount && amount > 0L && currencyExists;
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
