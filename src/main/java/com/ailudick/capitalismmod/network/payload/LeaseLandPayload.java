package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Requests leasing one owned chunk to an online player. */
public record LeaseLandPayload(String dimension, int chunkX, int chunkZ,
                               String targetName, long days, long rent) implements CustomPacketPayload {
    public static final Type<LeaseLandPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "lease_land"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LeaseLandPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.dimension());
                buf.writeVarInt(value.chunkX());
                buf.writeVarInt(value.chunkZ());
                buf.writeUtf(value.targetName());
                buf.writeVarLong(value.days());
                buf.writeVarLong(value.rent());
            },
            buf -> new LeaseLandPayload(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(),
                    buf.readVarLong(), buf.readVarLong()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
