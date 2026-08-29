package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: place a sell or buy order. Prices are in USD.
 *
 * @param sell {@code true} = sell order, {@code false} = buy order
 */
public record PlaceOrderPayload(int commodityIndex, int quantity, long pricePerUnit, boolean sell) implements CustomPacketPayload {
    public static final Type<PlaceOrderPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "place_order"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlaceOrderPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlaceOrderPayload::commodityIndex,
            ByteBufCodecs.VAR_INT, PlaceOrderPayload::quantity,
            ByteBufCodecs.VAR_LONG, PlaceOrderPayload::pricePerUnit,
            ByteBufCodecs.BOOL, PlaceOrderPayload::sell,
            PlaceOrderPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
