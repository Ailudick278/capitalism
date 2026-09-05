package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Synchronizes the trust list for the land currently being viewed. */
public record SyncLandTrustsPayload(int chunkX, int chunkZ, List<String> players)
        implements CustomPacketPayload {
    public static final Type<SyncLandTrustsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_land_trusts"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLandTrustsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.chunkX());
                buf.writeVarInt(value.chunkZ());
                buf.writeVarInt(value.players().size());
                value.players().forEach(buf::writeUtf);
            },
            buf -> {
                int chunkX = buf.readVarInt();
                int chunkZ = buf.readVarInt();
                int size = Math.min(buf.readVarInt(), 64);
                java.util.ArrayList<String> players = new java.util.ArrayList<>(size);
                for (int i = 0; i < size; i++) players.add(buf.readUtf(64));
                return new SyncLandTrustsPayload(chunkX, chunkZ, List.copyOf(players));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
