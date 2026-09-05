package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Requests adding or removing one online player from a land trust list. */
public record ManageLandTrustPayload(String dimension, int chunkX, int chunkZ,
                                     String targetName, boolean add) implements CustomPacketPayload {
    public static final Type<ManageLandTrustPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "manage_land_trust"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ManageLandTrustPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.dimension());
                buf.writeVarInt(value.chunkX());
                buf.writeVarInt(value.chunkZ());
                buf.writeUtf(value.targetName());
                buf.writeBoolean(value.add());
            },
            buf -> new ManageLandTrustPayload(buf.readUtf(), buf.readVarInt(), buf.readVarInt(),
                    buf.readUtf(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
