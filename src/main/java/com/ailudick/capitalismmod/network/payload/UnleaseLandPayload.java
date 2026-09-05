package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Requests ending the lease of one owned chunk. */
public record UnleaseLandPayload(String dimension, int chunkX, int chunkZ) implements CustomPacketPayload {
    public static final Type<UnleaseLandPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "unlease_land"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UnleaseLandPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeUtf(value.dimension()); buf.writeVarInt(value.chunkX()); buf.writeVarInt(value.chunkZ()); },
            buf -> new UnleaseLandPayload(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
