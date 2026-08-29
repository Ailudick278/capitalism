package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: withdraw {@code amount} USD from the futures margin account.
 */
public record WithdrawMarginPayload(long amount) implements CustomPacketPayload {
    public static final Type<WithdrawMarginPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "withdraw_margin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WithdrawMarginPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, WithdrawMarginPayload::amount,
            WithdrawMarginPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
