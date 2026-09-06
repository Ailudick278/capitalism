package com.ailudick.capitalismmod.tax;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaxRefundRulesTest {
    private static final long MINIMUM = 1L;
    private static final long AUTO_LIMIT = 100L;
    private static final long MAXIMUM = 1_000L;
    private static final long QUOTA = 3L;

    private static TaxRefundRules.Decision evaluate(boolean validCurrency, long amount, long availableCredit,
                                                     boolean pendingRequest, long requestsInPeriod) {
        return TaxRefundRules.evaluate(validCurrency, amount, availableCredit, pendingRequest, requestsInPeriod,
                MINIMUM, AUTO_LIMIT, MAXIMUM, QUOTA);
    }

    @Test
    void rejectsInvalidCurrencyAndPendingRequestsBeforeAmountChecks() {
        assertEquals(TaxRefundRules.Result.REJECTED,
                evaluate(false, 0L, 0L, false, 0).result());
        assertEquals(TaxRefundRules.Result.REJECTED,
                evaluate(true, MINIMUM, Long.MAX_VALUE, true, 0).result());
    }

    @Test
    void distinguishesAutomaticAndManualReviewLimits() {
        long automatic = AUTO_LIMIT;
        long maximum = MAXIMUM;
        long manual = automatic < maximum ? automatic + 1L : automatic;

        assertEquals(TaxRefundRules.Result.AUTO_APPROVED,
                evaluate(true, automatic, automatic, false, 0).result());
        if (manual <= maximum) {
            assertEquals(TaxRefundRules.Result.MANUAL_REVIEW,
                    evaluate(true, manual, manual, false, 0).result());
        }
    }

    @Test
    void rejectsInsufficientCreditAndRequestQuota() {
        long minimum = MINIMUM;
        assertEquals(TaxRefundRules.Result.REJECTED,
                evaluate(true, minimum, minimum - 1L, false, 0).result());
        assertEquals(TaxRefundRules.Result.REJECTED,
                evaluate(true, minimum, minimum, false, QUOTA).result());
    }
}
