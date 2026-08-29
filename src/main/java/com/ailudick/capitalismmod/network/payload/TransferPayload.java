package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: transfer money between two bank accounts by account number.
 */
public record TransferPayload(String fromAccountId, String targetAccountId, String currencyId, long amount) implements CustomPacketPayload {
    public static final Type<TransferPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "transfer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TransferPayload::fromAccountId,
            ByteBufCodecs.STRING_UTF8, TransferPayload::targetAccountId,
            ByteBufCodecs.STRING_UTF8, TransferPayload::currencyId,
            ByteBufCodecs.VAR_LONG, TransferPayload::amount,
            TransferPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
