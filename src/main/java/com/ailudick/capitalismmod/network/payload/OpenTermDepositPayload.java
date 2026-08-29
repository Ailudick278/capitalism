package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: open a fixed-term deposit.
 */
public record OpenTermDepositPayload(String accountId, String currencyId, long amount, int termDays) implements CustomPacketPayload {
    public static final Type<OpenTermDepositPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "open_term_deposit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTermDepositPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenTermDepositPayload::accountId,
            ByteBufCodecs.STRING_UTF8, OpenTermDepositPayload::currencyId,
            ByteBufCodecs.VAR_LONG, OpenTermDepositPayload::amount,
            ByteBufCodecs.VAR_INT, OpenTermDepositPayload::termDays,
            OpenTermDepositPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
