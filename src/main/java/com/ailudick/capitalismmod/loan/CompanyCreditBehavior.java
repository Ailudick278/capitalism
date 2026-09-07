package com.ailudick.capitalismmod.loan;

import net.minecraft.server.MinecraftServer;

/** Read-only repayment behaviour summary used by company credit reports. */
public record CompanyCreditBehavior(int payments, int onTimePayments, int overduePayments,
                                    long principalPaid, long interestPaid, int score) {
    /** Credit multiplier for businesses with repayment history. */
    public static double underwritingMultiplier(int payments, int score) {
        if (payments <= 0) return 1.0;
        return 0.50 + Math.max(0, Math.min(100, score)) / 200.0;
    }

    public double underwritingMultiplier() {
        return underwritingMultiplier(payments, score);
    }

    /** Annual credit spread for an established business, capped at 5 points. */
    public static double riskPremiumRate(int payments, int score) {
        if (payments <= 0) return 0.0;
        return (100 - Math.max(0, Math.min(100, score))) / 100.0 * 0.05;
    }

    public double riskPremiumRate() {
        return riskPremiumRate(payments, score);
    }

    /** Term multiplier: weak repayment history receives shorter maturities. */
    public static double termMultiplier(int payments, int score) {
        if (payments <= 0) return 1.0;
        return 0.50 + Math.max(0, Math.min(100, score)) / 200.0;
    }

    public double termMultiplier() {
        return termMultiplier(payments, score);
    }

    public static CompanyCreditBehavior from(MinecraftServer server, String companyId) {
        if (server == null || companyId == null || companyId.isBlank()) {
            return new CompanyCreditBehavior(0, 0, 0, 0L, 0L, 0);
        }
        int payments = 0;
        int onTime = 0;
        int overdue = 0;
        long principal = 0L;
        long interest = 0L;
        for (CompanyLoanPaymentSavedData.Payment payment
                : CompanyLoanPaymentSavedData.get(server).forCompany(companyId)) {
            if (payments < Integer.MAX_VALUE) payments++;
            if (payment.overdue()) {
                if (overdue < Integer.MAX_VALUE) overdue++;
            } else if (onTime < Integer.MAX_VALUE) {
                onTime++;
            }
            principal = addSaturated(principal, Math.max(0L, payment.principal()));
            interest = addSaturated(interest, Math.max(0L, payment.interest()));
        }
        int score;
        if (payments == 0) score = 0;
        else score = Math.max(0, Math.min(100, 100 - overdue * 25 + onTime * 2));
        return new CompanyCreditBehavior(payments, onTime, overdue, principal, interest, score);
    }

    private static long addSaturated(long left, long right) {
        if (left < 0L || right < 0L || left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }
}
