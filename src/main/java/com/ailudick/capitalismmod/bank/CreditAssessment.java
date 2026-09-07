package com.ailudick.capitalismmod.bank;

/** A deterministic credit assessment derived from existing account data. */
public record CreditAssessment(int score, long utilized, long approvedLimit, boolean overdue) {
    public static CreditAssessment evaluate(BankAccount account, long debtInBase, long configuredLimit) {
        return evaluate(account, debtInBase, configuredLimit, false);
    }

    public static CreditAssessment evaluate(BankAccount account, long debtInBase, long configuredLimit,
                                             boolean exposureStale) {
        long limit = Math.max(0L, configuredLimit);
        long debt = Math.max(0L, debtInBase);
        double utilization = limit == 0L ? 1.0 : Math.min(1.0, debt / (double) limit);
        int score = 700;
        if (utilization > 0.30) {
            score -= (int) Math.round(Math.min(180.0, (utilization - 0.30) / 0.70 * 180.0));
        }
        int repayments = 0;
        for (BankTransaction transaction : account.transactions()) {
            if ("repay".equals(transaction.type()) && transaction.amount() < 0L) repayments++;
        }
        score += Math.min(100, repayments * 10);
        if (debt == 0L && repayments > 0) score += 30;
        boolean overdue = debt > 0L && account.loanDaysRemaining() < 0;
        if (overdue) score -= 300;
        score = Math.max(300, Math.min(850, score));
        long approved = limit == 0L ? 0L : multiplyRatio(limit, score, 850L);
        if (exposureStale) approved /= 2L;
        return new CreditAssessment(score, debt, approved, overdue);
    }

    private static long multiplyRatio(long value, long numerator, long denominator) {
        if (value <= 0L || numerator <= 0L) return 0L;
        if (value > Long.MAX_VALUE / numerator) return Long.MAX_VALUE / denominator;
        return value * numerator / denominator;
    }
}
