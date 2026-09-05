package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncWorldMapPayload(String dimension, int chunkX, int chunkZ) implements CustomPacketPayload {
    public static final Type<SyncWorldMapPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_world_map"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncWorldMapPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeUtf(value.dimension()); buf.writeVarInt(value.chunkX()); buf.writeVarInt(value.chunkZ()); },
            buf -> new SyncWorldMapPayload(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
