package com.ailudick.capitalismmod.tax;

import com.ailudick.capitalismmod.Config;

/** Centralized built-in regulations used when a tax refund is submitted. */
public final class TaxRefundRules {
    private TaxRefundRules() {}

    public static Decision evaluate(boolean validCurrency, long amount, long availableCredit, boolean pendingRequest,
                                    long requestsInPeriod) {
        if (!validCurrency) return new Decision(Result.REJECTED, "Currency is not eligible for tax refunds.");
        if (amount < Config.TAX_REFUND_MIN_AMOUNT.get()) return new Decision(Result.REJECTED, "Refund amount is below the minimum.");
        if (pendingRequest) return new Decision(Result.REJECTED, "There is already a pending refund request.");
        if (amount > Config.TAX_REFUND_MAX_SINGLE_AMOUNT.get()) return new Decision(Result.REJECTED, "Refund exceeds the single-request limit.");
        if (requestsInPeriod >= Config.TAX_REFUND_MAX_REQUESTS_PER_PERIOD.get()) return new Decision(Result.REJECTED, "Refund request limit for this period has been reached.");
        if (availableCredit < amount) return new Decision(Result.REJECTED, "Refund exceeds available tax credit.");
        if (amount <= Config.TAX_REFUND_AUTO_APPROVAL_LIMIT.get()) {
            return new Decision(Result.AUTO_APPROVED, "Meets the automatic refund rules.");
        }
        return new Decision(Result.MANUAL_REVIEW, "Exceeds the automatic approval limit.");
    }

    public enum Result { AUTO_APPROVED, MANUAL_REVIEW, REJECTED }
    public record Decision(Result result, String reason) {}
}
