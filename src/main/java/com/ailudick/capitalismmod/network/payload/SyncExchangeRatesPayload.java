package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record SyncExchangeRatesPayload(Map<String, Long> anchors, String updatedAt, boolean live)
        implements CustomPacketPayload {
    public static final Type<SyncExchangeRatesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_exchange_rates"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncExchangeRatesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG),
            SyncExchangeRatesPayload::anchors, ByteBufCodecs.STRING_UTF8, SyncExchangeRatesPayload::updatedAt,
            ByteBufCodecs.BOOL, SyncExchangeRatesPayload::live, SyncExchangeRatesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
