package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: deposit {@code amount} USD into the futures margin account.
 */
public record DepositMarginPayload(long amount) implements CustomPacketPayload {
    public static final Type<DepositMarginPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "deposit_margin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DepositMarginPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, DepositMarginPayload::amount,
            DepositMarginPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
