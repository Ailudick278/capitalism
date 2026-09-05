package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncLandPermissionsPayload(String claimId, boolean memberBuild, boolean memberInteract,
                                         boolean container, boolean redstone)
        implements CustomPacketPayload {
    public static final Type<SyncLandPermissionsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_land_permissions"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLandPermissionsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeUtf(value.claimId()); buf.writeBoolean(value.memberBuild()); buf.writeBoolean(value.memberInteract());
                buf.writeBoolean(value.container()); buf.writeBoolean(value.redstone()); },
            buf -> new SyncLandPermissionsPayload(buf.readUtf(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
