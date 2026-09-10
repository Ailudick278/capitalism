package com.ailudick.capitalismmod.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client requests a snapshot; only the server computes the five era flags. */
public record TechnologySnapshotPayload(int mask) implements CustomPacketPayload {
    public static final Type<TechnologySnapshotPayload> TYPE=new Type<>(
            ResourceLocation.fromNamespaceAndPath("capitalismmod","technology_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf,TechnologySnapshotPayload> STREAM_CODEC=
            StreamCodec.of((buf,payload)->buf.writeInt(payload.mask()),buf->new TechnologySnapshotPayload(buf.readInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
