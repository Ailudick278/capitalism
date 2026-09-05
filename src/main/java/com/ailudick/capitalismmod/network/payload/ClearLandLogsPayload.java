package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClearLandLogsPayload() implements CustomPacketPayload {
    public static final Type<ClearLandLogsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "clear_land_logs"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearLandLogsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {}, buf -> new ClearLandLogsPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
