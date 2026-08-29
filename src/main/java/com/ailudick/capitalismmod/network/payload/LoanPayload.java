package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: take a loan or repay a loan on a credit account.
 *
 * @param repay {@code false} = take loan, {@code true} = repay
 */
public record LoanPayload(String accountId, String currencyId, long amount, boolean repay) implements CustomPacketPayload {
    public static final Type<LoanPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "loan"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LoanPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, LoanPayload::accountId,
            ByteBufCodecs.STRING_UTF8, LoanPayload::currencyId,
            ByteBufCodecs.VAR_LONG, LoanPayload::amount,
            ByteBufCodecs.BOOL, LoanPayload::repay,
            LoanPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
