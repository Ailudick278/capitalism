package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Requests authoritative land data for one chunk in the current dimension. */
public record RequestLandDetailsPayload(String dimension, int chunkX, int chunkZ)
        implements CustomPacketPayload {
    public static final Type<RequestLandDetailsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "request_land_details"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestLandDetailsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.dimension());
                buf.writeVarInt(value.chunkX());
                buf.writeVarInt(value.chunkZ());
            },
            buf -> new RequestLandDetailsPayload(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
