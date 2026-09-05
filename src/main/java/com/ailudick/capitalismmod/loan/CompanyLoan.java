package com.ailudick.capitalismmod.loan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A bank-style loan whose borrower is a company rather than a player. */
public record CompanyLoan(String id, String companyId, String currencyId, long principal,
                          double ratePerYear, int totalDays, int daysRemaining, long interestPaid) {
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
                Codec.LONG.optionalFieldOf("interestPaid", 0L).forGetter(CompanyLoan::interestPaid)
        ).apply(instance, CompanyLoan::new));
    }

    public CompanyLoan withDaysRemaining(int value) {
        return new CompanyLoan(id, companyId, currencyId, principal, ratePerYear,
                totalDays, value, interestPaid);
    }

    public CompanyLoan withCompanyId(String newCompanyId) {
        return new CompanyLoan(id, newCompanyId, currencyId, principal, ratePerYear,
                totalDays, daysRemaining, interestPaid);
    }

    public CompanyLoan withInterestPaid(long value) {
        return new CompanyLoan(id, companyId, currencyId, principal, ratePerYear,
                totalDays, daysRemaining, Math.max(0L, value));
    }

    public CompanyLoan withPrincipal(long value) {
        return new CompanyLoan(id, companyId, currencyId, Math.max(0L, value), ratePerYear,
                totalDays, daysRemaining, interestPaid);
    }

    public long interestDue() {
        if (principal <= 0L || totalDays <= 0 || !Double.isFinite(ratePerYear) || ratePerYear < 0.0) return 0L;
        int elapsed = Math.max(0, totalDays - daysRemaining);
        double multiplier = daysRemaining < 0 ? 2.0 : 1.0;
        double interest = principal * ratePerYear / 365.0 * elapsed * multiplier;
        if (!Double.isFinite(interest) || interest >= Long.MAX_VALUE) return Long.MAX_VALUE;
        long accrued = Math.max(0L, (long) interest);
        return accrued > interestPaid ? accrued - interestPaid : 0L;
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
