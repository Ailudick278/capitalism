package com.ailudick.capitalismmod.loan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

/**
 * A peer-to-peer loan between two players.
 *
 * @param principal     amount lent, in major units of {@code currencyId}
 * @param ratePerYear   annual interest rate as a fraction (0.05 = 5%)
 * @param totalDays     original term in Minecraft days
 * @param daysRemaining days until maturity (negative = overdue)
 */
public record PeerLoan(String id, UUID lender, UUID borrower, String currencyId, long principal, double ratePerYear, int totalDays, int daysRemaining,
                       long interestPaid, long lastSettlementDay, long accruedInterest, long principalChangeElapsed) {

    public PeerLoan(String id, UUID lender, UUID borrower, String currencyId, long principal,
                    double ratePerYear, int totalDays, int daysRemaining) {
        this(id, lender, borrower, currencyId, principal, ratePerYear, totalDays, daysRemaining, 0L, -1L);
    }

    public PeerLoan(String id, UUID lender, UUID borrower, String currencyId, long principal,
                    double ratePerYear, int totalDays, int daysRemaining, long interestPaid) {
        this(id, lender, borrower, currencyId, principal, ratePerYear, totalDays, daysRemaining, interestPaid, -1L);
    }

    public PeerLoan(String id, UUID lender, UUID borrower, String currencyId, long principal,
                    double ratePerYear, int totalDays, int daysRemaining, long interestPaid,
                    long lastSettlementDay) {
        this(id, lender, borrower, currencyId, principal, ratePerYear, totalDays, daysRemaining,
                interestPaid, lastSettlementDay, 0L, 0L);
    }

    /**
     * Codec construction is lazy so pure loan calculations do not require the
     * Minecraft data-fixer runtime to be present. Persistence asks for the
     * codec explicitly through this method.
     */
    public static Codec<PeerLoan> codec() {
        return Codecs.CODEC;
    }

    private static final class Codecs {
        private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
        private static final Codec<PeerLoan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(PeerLoan::id),
                UUID_CODEC.fieldOf("lender").forGetter(PeerLoan::lender),
                UUID_CODEC.fieldOf("borrower").forGetter(PeerLoan::borrower),
                Codec.STRING.fieldOf("currencyId").forGetter(PeerLoan::currencyId),
                Codec.LONG.fieldOf("principal").forGetter(PeerLoan::principal),
                Codec.DOUBLE.fieldOf("ratePerYear").forGetter(PeerLoan::ratePerYear),
                Codec.INT.fieldOf("totalDays").forGetter(PeerLoan::totalDays),
                Codec.INT.fieldOf("daysRemaining").forGetter(PeerLoan::daysRemaining),
                Codec.LONG.optionalFieldOf("interestPaid", 0L).forGetter(PeerLoan::interestPaid),
                Codec.LONG.optionalFieldOf("lastSettlementDay", -1L).forGetter(PeerLoan::lastSettlementDay),
                Codec.LONG.optionalFieldOf("accruedInterest", 0L).forGetter(PeerLoan::accruedInterest),
                Codec.LONG.optionalFieldOf("principalChangeElapsed", 0L).forGetter(PeerLoan::principalChangeElapsed)
        ).apply(instance, PeerLoan::new));
    }

    public PeerLoan withDaysRemaining(int newDays) {
        return new PeerLoan(id, lender, borrower, currencyId, principal, ratePerYear, totalDays, newDays, interestPaid,
                lastSettlementDay, accruedInterest, principalChangeElapsed);
    }

    public PeerLoan withInterestPaid(long amount) {
        return new PeerLoan(id, lender, borrower, currencyId, principal, ratePerYear, totalDays,
                daysRemaining, Math.max(0L, amount), lastSettlementDay, accruedInterest, principalChangeElapsed);
    }

    public PeerLoan withLastSettlementDay(long day) {
        return new PeerLoan(id, lender, borrower, currencyId, principal, ratePerYear, totalDays,
                daysRemaining, interestPaid, day, accruedInterest, principalChangeElapsed);
    }

    public PeerLoan withPrincipal(long value) {
        return new PeerLoan(id, lender, borrower, currencyId, Math.max(0L, value), ratePerYear, totalDays,
                daysRemaining, interestPaid, lastSettlementDay, accruedInterest, principalChangeElapsed);
    }

    public PeerLoan withInterestAccrualState(long newAccruedInterest, long newPrincipalChangeElapsed) {
        return new PeerLoan(id, lender, borrower, currencyId, principal, ratePerYear, totalDays, daysRemaining,
                interestPaid, lastSettlementDay, Math.max(0L, newAccruedInterest), Math.max(0L, newPrincipalChangeElapsed));
    }

    public long totalInterestAccrued() {
        if (principal <= 0L || totalDays <= 0 || !Double.isFinite(ratePerYear) || ratePerYear < 0.0) return accruedInterest;
        long elapsed = Math.max(0L, (long) totalDays - daysRemaining);
        long start = Math.min(Math.max(0L, principalChangeElapsed), elapsed);
        long segment = interestBetween(start, elapsed, principal, ratePerYear, totalDays);
        return accruedInterest > Long.MAX_VALUE - segment ? Long.MAX_VALUE : accruedInterest + segment;
    }

    /** Interest currently due (major units); only overdue days carry penalty interest. */
    public long interestDue() {
        long accrued = totalInterestAccrued();
        return accrued > interestPaid ? accrued - interestPaid : 0L;
    }

    public boolean isOverdue() {
        return daysRemaining < 0;
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
}
