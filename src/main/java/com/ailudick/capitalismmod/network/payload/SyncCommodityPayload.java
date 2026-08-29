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
 * Server -> Client: sync commodity prices and candle history.
 */
public record SyncCommodityPayload(Map<String, Long> prices, Map<String, List<Candle>> history) implements CustomPacketPayload {
    public static final Type<SyncCommodityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_commodity"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCommodityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG), SyncCommodityPayload::prices,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, Candle.STREAM_CODEC.apply(ByteBufCodecs.list())), SyncCommodityPayload::history,
            SyncCommodityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
