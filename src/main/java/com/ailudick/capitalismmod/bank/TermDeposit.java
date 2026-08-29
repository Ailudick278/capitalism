package com.ailudick.capitalismmod.bank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A fixed-term deposit (定期存款). Money is locked for {@code daysRemaining} Minecraft days
 * and pays {@code interest} on maturity; early withdrawal forfeits the interest.
 *
 * @param currencyId   currency of the deposit
 * @param principal    amount deposited
 * @param interest     total interest paid on maturity (computed at open time)
 * @param daysRemaining Minecraft days until maturity
 */
public record TermDeposit(String currencyId, long principal, long interest, int daysRemaining) {

    public static final Codec<TermDeposit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("currencyId").forGetter(TermDeposit::currencyId),
            Codec.LONG.fieldOf("principal").forGetter(TermDeposit::principal),
            Codec.LONG.fieldOf("interest").forGetter(TermDeposit::interest),
            Codec.INT.fieldOf("daysRemaining").forGetter(TermDeposit::daysRemaining)
    ).apply(instance, TermDeposit::new));

    public TermDeposit tick() {
        return new TermDeposit(currencyId, principal, interest, daysRemaining - 1);
    }
}
