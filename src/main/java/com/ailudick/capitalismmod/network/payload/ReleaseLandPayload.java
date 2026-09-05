package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Requests releasing one owned chunk; the server validates ownership. */
public record ReleaseLandPayload(String dimension, int chunkX, int chunkZ) implements CustomPacketPayload {
    public static final Type<ReleaseLandPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "release_land"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReleaseLandPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.dimension());
                buf.writeVarInt(value.chunkX());
                buf.writeVarInt(value.chunkZ());
            },
            buf -> new ReleaseLandPayload(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
