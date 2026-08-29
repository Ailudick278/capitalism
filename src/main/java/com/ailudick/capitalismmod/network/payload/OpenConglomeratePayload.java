package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: open the conglomerate GUI.
 */
public record OpenConglomeratePayload() implements CustomPacketPayload {
    public static final Type<OpenConglomeratePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "open_conglomerate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenConglomeratePayload> STREAM_CODEC =
            StreamCodec.unit(new OpenConglomeratePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
