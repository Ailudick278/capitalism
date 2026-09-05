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
                       long interestPaid) {

    public PeerLoan(String id, UUID lender, UUID borrower, String currencyId, long principal,
                    double ratePerYear, int totalDays, int daysRemaining) {
        this(id, lender, borrower, currencyId, principal, ratePerYear, totalDays, daysRemaining, 0L);
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
                Codec.LONG.optionalFieldOf("interestPaid", 0L).forGetter(PeerLoan::interestPaid)
        ).apply(instance, PeerLoan::new));
    }

    public PeerLoan withDaysRemaining(int newDays) {
        return new PeerLoan(id, lender, borrower, currencyId, principal, ratePerYear, totalDays, newDays, interestPaid);
    }

    public PeerLoan withInterestPaid(long amount) {
        return new PeerLoan(id, lender, borrower, currencyId, principal, ratePerYear, totalDays,
                daysRemaining, Math.max(0L, amount));
    }

    /** Interest currently due (major units); only overdue days carry penalty interest. */
    public long interestDue() {
        if (principal <= 0 || totalDays <= 0 || !Double.isFinite(ratePerYear) || ratePerYear < 0) {
            return 0;
        }
        long elapsed = Math.max(0L, (long) totalDays - daysRemaining);
        long regularDays = Math.min(elapsed, (long) totalDays);
        long overdueDays = Math.max(0L, elapsed - totalDays);
        double dailyInterest = principal * ratePerYear / 365.0;
        double interest = dailyInterest * regularDays + dailyInterest * overdueDays * 2.0;
        if (!Double.isFinite(interest) || interest >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        long accrued = Math.max(0, (long) interest);
        return accrued > interestPaid ? accrued - interestPaid : 0L;
    }
}
