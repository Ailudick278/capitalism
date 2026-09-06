package com.ailudick.capitalismmod.loan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A bank-style loan whose borrower is a company rather than a player. */
public record CompanyLoan(String id, String companyId, String currencyId, long principal,
                          double ratePerYear, int totalDays, int daysRemaining, long interestPaid,
                          long lastSettlementDay, long accruedInterest, long principalChangeElapsed) {
    public CompanyLoan(String id, String companyId, String currencyId, long principal,
                       double ratePerYear, int totalDays, int daysRemaining, long interestPaid) {
        this(id, companyId, currencyId, principal, ratePerYear, totalDays, daysRemaining, interestPaid,
                -1L, 0L, 0L);
    }

    public CompanyLoan(String id, String companyId, String currencyId, long principal,
                       double ratePerYear, int totalDays, int daysRemaining, long interestPaid,
                       long lastSettlementDay) {
        this(id, companyId, currencyId, principal, ratePerYear, totalDays, daysRemaining, interestPaid,
                lastSettlementDay, 0L, 0L);
    }
    /** Lazy like the peer-loan codec, so pure interest calculations work in unit tests. */
    public static Codec<CompanyLoan> codec() { return Codecs.CODEC; }

    private static final class Codecs {
        private static final Codec<CompanyLoan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(CompanyLoan::id),
                Codec.STRING.fieldOf("companyId").forGetter(CompanyLoan::companyId),
                Codec.STRING.fieldOf("currencyId").forGetter(CompanyLoan::currencyId),
                Codec.LONG.fieldOf("principal").forGetter(CompanyLoan::principal),
                Codec.DOUBLE.fieldOf("ratePerYear").forGetter(CompanyLoan::ratePerYear),
                Codec.INT.fieldOf("totalDays").forGetter(CompanyLoan::totalDays),
                Codec.INT.fieldOf("daysRemaining").forGetter(CompanyLoan::daysRemaining),
                Codec.LONG.optionalFieldOf("interestPaid", 0L).forGetter(CompanyLoan::interestPaid),
                Codec.LONG.optionalFieldOf("lastSettlementDay", -1L).forGetter(CompanyLoan::lastSettlementDay),
                Codec.LONG.optionalFieldOf("accruedInterest", 0L).forGetter(CompanyLoan::accruedInterest),
                Codec.LONG.optionalFieldOf("principalChangeElapsed", 0L).forGetter(CompanyLoan::principalChangeElapsed)
        ).apply(instance, CompanyLoan::new));
    }

    public CompanyLoan withDaysRemaining(int value) {
        return new CompanyLoan(id, companyId, currencyId, principal, ratePerYear,
                totalDays, value, interestPaid, lastSettlementDay, accruedInterest, principalChangeElapsed);
    }

    public CompanyLoan withCompanyId(String newCompanyId) {
        return new CompanyLoan(id, newCompanyId, currencyId, principal, ratePerYear,
                totalDays, daysRemaining, interestPaid, lastSettlementDay, accruedInterest, principalChangeElapsed);
    }

    public CompanyLoan withInterestPaid(long value) {
        return new CompanyLoan(id, companyId, currencyId, principal, ratePerYear,
                totalDays, daysRemaining, Math.max(0L, value), lastSettlementDay, accruedInterest, principalChangeElapsed);
    }

    public CompanyLoan withPrincipal(long value) {
        return new CompanyLoan(id, companyId, currencyId, Math.max(0L, value), ratePerYear,
                totalDays, daysRemaining, interestPaid, lastSettlementDay, accruedInterest, principalChangeElapsed);
    }

    public CompanyLoan withLastSettlementDay(long value) {
        return new CompanyLoan(id, companyId, currencyId, principal, ratePerYear,
                totalDays, daysRemaining, interestPaid, value, accruedInterest, principalChangeElapsed);
    }

    public CompanyLoan withInterestAccrualState(long newAccruedInterest, long newPrincipalChangeElapsed) {
        return new CompanyLoan(id, companyId, currencyId, principal, ratePerYear, totalDays, daysRemaining,
                interestPaid, lastSettlementDay, Math.max(0L, newAccruedInterest), Math.max(0L, newPrincipalChangeElapsed));
    }

    public long totalInterestAccrued() {
        if (principal <= 0L || totalDays <= 0 || !Double.isFinite(ratePerYear) || ratePerYear < 0.0) return accruedInterest;
        long elapsed = Math.max(0L, (long) totalDays - daysRemaining);
        long start = Math.min(Math.max(0L, principalChangeElapsed), elapsed);
        long segment = interestBetween(start, elapsed, principal, ratePerYear, totalDays);
        return accruedInterest > Long.MAX_VALUE - segment ? Long.MAX_VALUE : accruedInterest + segment;
    }

    public long interestDue() {
        long accrued = totalInterestAccrued();
        return accrued > interestPaid ? accrued - interestPaid : 0L;
    }

    private static long interestBetween(long startElapsed, long endElapsed, long principal,
                                        double ratePerYear, int totalDays) {
        if (endElapsed <= startElapsed) return 0L;
        long regularDays = Math.max(0L, Math.min(endElapsed, (long) totalDays)
                - Math.min(startElapsed, (long) totalDays));
        long overdueDays = Math.max(0L, endElapsed - Math.max(startElapsed, (long) totalDays));
        double interest = principal * ratePerYear / 365.0 * (regularDays + overdueDays * 2.0);
        if (!Double.isFinite(interest) || interest >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(0L, (long) interest);
    }

    /** Equal-payment estimate for the remaining term; overdue loans are immediately due. */
    public long scheduledPayment() {
        long total;
        try {
            total = Math.addExact(Math.max(0L, principal), interestDue());
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
        long periods = Math.max(1L, daysRemaining);
        return total / periods + (total % periods == 0L ? 0L : 1L);
    }

    public boolean isOverdue() {
        return daysRemaining < 0;
    }

    public boolean becomesDueAfter(int nextDaysRemaining) {
        return daysRemaining > 0 && nextDaysRemaining <= 0;
    }

    public boolean becomesOverdueAfter(int nextDaysRemaining) {
        return daysRemaining >= 0 && nextDaysRemaining < 0;
    }
}
