package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetLandPermissionsPayload(String dimension, int chunkX, int chunkZ,
                                        boolean memberBuild, boolean memberInteract, boolean container, boolean redstone) implements CustomPacketPayload {
    public static final Type<SetLandPermissionsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "set_land_permissions"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetLandPermissionsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeUtf(value.dimension()); buf.writeVarInt(value.chunkX()); buf.writeVarInt(value.chunkZ());
                buf.writeBoolean(value.memberBuild()); buf.writeBoolean(value.memberInteract());
                buf.writeBoolean(value.container()); buf.writeBoolean(value.redstone()); },
            buf -> new SetLandPermissionsPayload(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
