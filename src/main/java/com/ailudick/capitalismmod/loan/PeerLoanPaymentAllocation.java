package com.ailudick.capitalismmod.loan;

import java.util.Optional;

/** Pure allocation rule for peer-loan payments: accrued interest before principal. */
public record PeerLoanPaymentAllocation(long payment, long interestPayment,
                                        long principalPayment, long remainingPrincipal) {
    public static Optional<PeerLoanPaymentAllocation> forAmount(PeerLoan loan, long requestedAmount) {
        if (loan == null || requestedAmount <= 0L) return Optional.empty();
        long interest = loan.interestDue();
        long total = saturatedAdd(loan.principal(), interest);
        if (total < 0L || requestedAmount > total) return Optional.empty();
        long interestPayment = Math.min(requestedAmount, interest);
        long principalPayment = requestedAmount - interestPayment;
        if (principalPayment < 0L || principalPayment > loan.principal()) return Optional.empty();
        return Optional.of(new PeerLoanPaymentAllocation(requestedAmount, interestPayment,
                principalPayment, loan.principal() - principalPayment));
    }

    private static long saturatedAdd(long left, long right) {
        if (left < 0L || right < 0L) return -1L;
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
