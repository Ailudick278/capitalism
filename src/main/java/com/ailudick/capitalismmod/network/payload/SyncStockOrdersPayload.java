package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.stock.StockOrder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server -> Client: sync the stock order book.
 */
public record SyncStockOrdersPayload(List<StockOrder> orders) implements CustomPacketPayload {
    public static final Type<SyncStockOrdersPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_stock_orders"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncStockOrdersPayload> STREAM_CODEC = StreamCodec.composite(
            StockOrder.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncStockOrdersPayload::orders,
            SyncStockOrdersPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
