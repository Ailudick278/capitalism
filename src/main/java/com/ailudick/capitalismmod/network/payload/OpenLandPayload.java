package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenLandPayload() implements CustomPacketPayload {
    public static final Type<OpenLandPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "open_land"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLandPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenLandPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
