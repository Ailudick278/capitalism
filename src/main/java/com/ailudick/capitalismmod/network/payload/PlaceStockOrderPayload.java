package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: place a limit order on the stock exchange.
 *
 * @param sell {@code true} = sell order, {@code false} = buy order
 */
public record PlaceStockOrderPayload(String stockId, int quantity, long pricePerUnit, boolean sell) implements CustomPacketPayload {
    public static final Type<PlaceStockOrderPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "place_stock_order"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlaceStockOrderPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PlaceStockOrderPayload::stockId,
            ByteBufCodecs.VAR_INT, PlaceStockOrderPayload::quantity,
            ByteBufCodecs.VAR_LONG, PlaceStockOrderPayload::pricePerUnit,
            ByteBufCodecs.BOOL, PlaceStockOrderPayload::sell,
            PlaceStockOrderPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
