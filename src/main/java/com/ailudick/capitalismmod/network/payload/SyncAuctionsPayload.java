package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.auction.Auction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server -> Client: sync the active auction list.
 */
public record SyncAuctionsPayload(List<Auction> auctions) implements CustomPacketPayload {
    public static final Type<SyncAuctionsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_auctions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAuctionsPayload> STREAM_CODEC = StreamCodec.composite(
            Auction.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncAuctionsPayload::auctions,
            SyncAuctionsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
