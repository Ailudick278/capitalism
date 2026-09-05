package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Reserved for the future land UI; currently not registered or sent by the map. */
public record OpenLandAtChunkPayload(String dimension, int chunkX, int chunkZ) implements CustomPacketPayload {
    public static final Type<OpenLandAtChunkPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "open_land_at_chunk"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLandAtChunkPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeUtf(value.dimension()); buf.writeVarInt(value.chunkX()); buf.writeVarInt(value.chunkZ()); },
            buf -> new OpenLandAtChunkPayload(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
