package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: place a purchase order against a supplier's offer.
 */
public record PlaceSupplyOrderPayload(String offerId, int quantity, String companyName) implements CustomPacketPayload {
    public static final Type<PlaceSupplyOrderPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "place_supply_order"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlaceSupplyOrderPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PlaceSupplyOrderPayload::offerId,
            ByteBufCodecs.VAR_INT, PlaceSupplyOrderPayload::quantity,
            ByteBufCodecs.STRING_UTF8, PlaceSupplyOrderPayload::companyName,
            PlaceSupplyOrderPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
