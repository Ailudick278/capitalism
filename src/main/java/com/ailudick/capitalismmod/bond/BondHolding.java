package com.ailudick.capitalismmod.bond;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * A government bond held by a player.
 *
 * @param faceValue      par value in USD
 * @param ratePerYear    annual coupon rate (fraction, 0.05 = 5%)
 * @param totalDays      original term in Minecraft days
 * @param daysToMaturity days until maturity
 */
public record BondHolding(String id, UUID holder, long faceValue, double ratePerYear, int totalDays, int daysToMaturity) {

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<BondHolding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(BondHolding::id),
            UUID_CODEC.fieldOf("holder").forGetter(BondHolding::holder),
            Codec.LONG.fieldOf("faceValue").forGetter(BondHolding::faceValue),
            Codec.DOUBLE.fieldOf("ratePerYear").forGetter(BondHolding::ratePerYear),
            Codec.INT.fieldOf("totalDays").forGetter(BondHolding::totalDays),
            Codec.INT.fieldOf("daysToMaturity").forGetter(BondHolding::daysToMaturity)
    ).apply(instance, BondHolding::new));

    public BondHolding withDaysToMaturity(int newDays) {
        return new BondHolding(id, holder, faceValue, ratePerYear, totalDays, newDays);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, BondHolding> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BondHolding::id,
            ByteBufCodecs.STRING_UTF8, h -> h.holder().toString(),
            ByteBufCodecs.VAR_LONG, BondHolding::faceValue,
            ByteBufCodecs.DOUBLE, BondHolding::ratePerYear,
            ByteBufCodecs.VAR_INT, BondHolding::totalDays,
            ByteBufCodecs.VAR_INT, BondHolding::daysToMaturity,
            (id, holder, faceValue, ratePerYear, totalDays, daysToMaturity) ->
                    new BondHolding(id, UUID.fromString(holder), faceValue, ratePerYear, totalDays, daysToMaturity));

    /** Interest accrued so far (USD), for early redemption. */
    public long accruedInterest() {
        int elapsed = Math.max(0, totalDays - daysToMaturity);
        return (long) (faceValue * ratePerYear / 365.0 * elapsed);
    }
}
