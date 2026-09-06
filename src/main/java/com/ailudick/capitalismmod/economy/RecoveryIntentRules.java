package com.ailudick.capitalismmod.economy;

import java.util.UUID;

/** Pure validation rules for player-bound recovery intents and loan receipts. */
public final class RecoveryIntentRules {
    private RecoveryIntentRules() {
    }

    public static boolean validExchange(String id, UUID player, String from, String to,
                                        long amount, long converted, boolean fromExists, boolean toExists) {
        return nonBlank(id) && player != null && nonBlank(from) && nonBlank(to) && !from.equals(to)
                && amount > 0L && converted > 0L && fromExists && toExists;
    }

    public static boolean validTransfer(String id, UUID sender, UUID target, String currency,
                                        long amount, boolean currencyExists) {
        return nonBlank(id) && sender != null && target != null && !sender.equals(target)
                && nonBlank(currency) && amount > 0L && currencyExists;
    }

    public static boolean validOrigination(String loanId, UUID lender, UUID borrower, String currency,
                                           long principal, double ratePerYear, int days,
                                           boolean currencyExists) {
        return nonBlank(loanId) && lender != null && borrower != null && !lender.equals(borrower)
                && nonBlank(currency) && principal > 0L && Double.isFinite(ratePerYear)
                && ratePerYear >= 0.0 && days > 0 && currencyExists;
    }

    public static boolean validPayment(String loanId, UUID lender, UUID borrower, String currency,
                                       long timestamp, long total, long interest, long principal,
                                       long remainingPrincipal, int daysRemaining, boolean currencyExists) {
        return nonBlank(loanId) && lender != null && borrower != null && !lender.equals(borrower)
                && nonBlank(currency) && timestamp >= 0L && total > 0L && interest >= 0L
                && principal >= 0L && remainingPrincipal >= 0L && daysRemaining >= 0
                && safeAdd(interest, principal) == total && currencyExists;
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static long safeAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            return Long.MIN_VALUE;
        }
    }
}
