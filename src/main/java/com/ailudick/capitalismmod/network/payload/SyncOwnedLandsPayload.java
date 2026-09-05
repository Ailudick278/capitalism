package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Synchronizes the player's owned land index for the land management panel. */
public record SyncOwnedLandsPayload(List<LandEntry> lands) implements CustomPacketPayload {
    public record LandEntry(String dimension, int chunkX, int chunkZ, String purpose, boolean leased, long debt) {}

    public static final Type<SyncOwnedLandsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_owned_lands"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncOwnedLandsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.lands().size());
                value.lands().forEach(land -> {
                    buf.writeUtf(land.dimension()); buf.writeVarInt(land.chunkX()); buf.writeVarInt(land.chunkZ());
                    buf.writeUtf(land.purpose()); buf.writeBoolean(land.leased()); buf.writeVarLong(land.debt());
                });
            },
            buf -> {
                int size = Math.min(buf.readVarInt(), 4096);
                java.util.ArrayList<LandEntry> lands = new java.util.ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    lands.add(new LandEntry(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(),
                            buf.readBoolean(), buf.readVarLong()));
                }
                return new SyncOwnedLandsPayload(List.copyOf(lands));
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
