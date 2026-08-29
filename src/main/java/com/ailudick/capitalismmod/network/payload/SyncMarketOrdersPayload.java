package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.market.MarketOrder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server -> Client: sync the market order book.
 */
public record SyncMarketOrdersPayload(List<MarketOrder> orders) implements CustomPacketPayload {
    public static final Type<SyncMarketOrdersPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_market_orders"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMarketOrdersPayload> STREAM_CODEC = StreamCodec.composite(
            MarketOrder.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncMarketOrdersPayload::orders,
            SyncMarketOrdersPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
