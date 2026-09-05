package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenWorldMapPayload() implements CustomPacketPayload {
    public static final Type<OpenWorldMapPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "open_world_map"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenWorldMapPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenWorldMapPayload());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
