package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.stock.Candle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server -> Client: sync stock prices, candle history, and the player's portfolio.
 */
public record SyncStocksPayload(Map<String, Long> prices, Map<String, Long> portfolio, Map<String, List<Candle>> history, Map<String, String> companies) implements CustomPacketPayload {
    public static final Type<SyncStocksPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_stocks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncStocksPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG), SyncStocksPayload::prices,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG), SyncStocksPayload::portfolio,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, Candle.STREAM_CODEC.apply(ByteBufCodecs.list())), SyncStocksPayload::history,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8), SyncStocksPayload::companies,
            SyncStocksPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
