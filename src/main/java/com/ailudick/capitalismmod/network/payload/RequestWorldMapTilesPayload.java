package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestWorldMapTilesPayload(int centerChunkX, int centerChunkZ, int radius, boolean discover) implements CustomPacketPayload {
    public static final Type<RequestWorldMapTilesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "request_world_map_tiles"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestWorldMapTilesPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeVarInt(value.centerChunkX()); buf.writeVarInt(value.centerChunkZ()); buf.writeVarInt(value.radius()); buf.writeBoolean(value.discover()); },
            buf -> new RequestWorldMapTilesPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
