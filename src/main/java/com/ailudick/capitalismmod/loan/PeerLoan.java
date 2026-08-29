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
public record PeerLoan(String id, UUID lender, UUID borrower, String currencyId, long principal, double ratePerYear, int totalDays, int daysRemaining) {

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<PeerLoan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(PeerLoan::id),
            UUID_CODEC.fieldOf("lender").forGetter(PeerLoan::lender),
            UUID_CODEC.fieldOf("borrower").forGetter(PeerLoan::borrower),
            Codec.STRING.fieldOf("currencyId").forGetter(PeerLoan::currencyId),
            Codec.LONG.fieldOf("principal").forGetter(PeerLoan::principal),
            Codec.DOUBLE.fieldOf("ratePerYear").forGetter(PeerLoan::ratePerYear),
            Codec.INT.fieldOf("totalDays").forGetter(PeerLoan::totalDays),
            Codec.INT.fieldOf("daysRemaining").forGetter(PeerLoan::daysRemaining)
    ).apply(instance, PeerLoan::new));

    public PeerLoan withDaysRemaining(int newDays) {
        return new PeerLoan(id, lender, borrower, currencyId, principal, ratePerYear, totalDays, newDays);
    }

    /** Interest currently due (major units), doubled when overdue. */
    public long interestDue() {
        int elapsed = Math.max(0, totalDays - daysRemaining);
        double multiplier = daysRemaining < 0 ? 2.0 : 1.0;
        return (long) (principal * ratePerYear / 365.0 * elapsed * multiplier);
    }
}
