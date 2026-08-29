package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: buy {@code count} government bonds at face value.
 */
public record BuyBondPayload(int count) implements CustomPacketPayload {
    public static final Type<BuyBondPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "buy_bond"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BuyBondPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BuyBondPayload::count,
            BuyBondPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
