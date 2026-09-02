package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: exchange {@code amount} from one selected bank account currency into another.
 */
public record ExchangePayload(String accountId, String from, String to, long amount) implements CustomPacketPayload {
    public static final Type<ExchangePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "exchange"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExchangePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ExchangePayload::accountId,
            ByteBufCodecs.STRING_UTF8, ExchangePayload::from,
            ByteBufCodecs.STRING_UTF8, ExchangePayload::to,
            ByteBufCodecs.VAR_LONG, ExchangePayload::amount,
            ExchangePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
