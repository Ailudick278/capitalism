package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncLandSalePayload(boolean active, String target, long price, long expiresAt,
                                  boolean auctionActive, long auctionStartPrice, long auctionHighestBid,
                                  String auctionBidder, long auctionEndsAt)
        implements CustomPacketPayload {
    public static final Type<SyncLandSalePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_land_sale"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLandSalePayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeBoolean(value.active()); buf.writeUtf(value.target());
                buf.writeVarLong(value.price()); buf.writeVarLong(value.expiresAt());
                buf.writeBoolean(value.auctionActive()); buf.writeVarLong(value.auctionStartPrice());
                buf.writeVarLong(value.auctionHighestBid()); buf.writeUtf(value.auctionBidder());
                buf.writeVarLong(value.auctionEndsAt()); },
            buf -> new SyncLandSalePayload(buf.readBoolean(), buf.readUtf(), buf.readVarLong(), buf.readVarLong(),
                    buf.readBoolean(), buf.readVarLong(), buf.readVarLong(), buf.readUtf(), buf.readVarLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
