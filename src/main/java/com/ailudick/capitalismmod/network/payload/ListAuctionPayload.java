package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: list a commodity for auction.
 */
public record ListAuctionPayload(int commodityIndex, int quantity, long startingPrice, int durationSeconds) implements CustomPacketPayload {
    public static final Type<ListAuctionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "list_auction"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ListAuctionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ListAuctionPayload::commodityIndex,
            ByteBufCodecs.VAR_INT, ListAuctionPayload::quantity,
            ByteBufCodecs.VAR_LONG, ListAuctionPayload::startingPrice,
            ByteBufCodecs.VAR_INT, ListAuctionPayload::durationSeconds,
            ListAuctionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
