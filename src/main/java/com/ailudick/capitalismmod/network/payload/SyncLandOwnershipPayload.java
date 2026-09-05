package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record SyncLandOwnershipPayload(List<String> owners) implements CustomPacketPayload {
    public static final Type<SyncLandOwnershipPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_land_ownership"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLandOwnershipPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeVarInt(value.owners().size()); value.owners().forEach(buf::writeUtf); },
            buf -> { int size = Math.min(buf.readVarInt(), 64); java.util.ArrayList<String> owners = new java.util.ArrayList<>(size);
                for (int i = 0; i < size; i++) owners.add(buf.readUtf(64)); return new SyncLandOwnershipPayload(List.copyOf(owners)); });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
