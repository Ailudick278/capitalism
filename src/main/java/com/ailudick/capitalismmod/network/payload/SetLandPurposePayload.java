package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Requests changing the purpose of one owned chunk. */
public record SetLandPurposePayload(String dimension, int chunkX, int chunkZ, String purpose)
        implements CustomPacketPayload {
    public static final Type<SetLandPurposePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "set_land_purpose"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetLandPurposePayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.dimension());
                buf.writeVarInt(value.chunkX());
                buf.writeVarInt(value.chunkZ());
                buf.writeUtf(value.purpose());
            },
            buf -> new SetLandPurposePayload(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readUtf()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
