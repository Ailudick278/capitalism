package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Land ownership overlay for the world map; deliberately separate from land details. */
public record SyncLandOverlayPayload(String dimension, List<Cell> cells) implements CustomPacketPayload {
    public record Cell(int chunkX, int chunkZ, boolean claimed, boolean ownedByPlayer, boolean auction) {}

    public static final Type<SyncLandOverlayPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_land_overlay"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLandOverlayPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.dimension());
                buf.writeVarInt(value.cells().size());
                value.cells().forEach(cell -> {
                    buf.writeVarInt(cell.chunkX());
                    buf.writeVarInt(cell.chunkZ());
                    buf.writeBoolean(cell.claimed());
                    buf.writeBoolean(cell.ownedByPlayer());
                    buf.writeBoolean(cell.auction());
                });
            },
            buf -> {
                String dimension = buf.readUtf();
                int size = Math.min(buf.readVarInt(), 625);
                java.util.ArrayList<Cell> cells = new java.util.ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    cells.add(new Cell(buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));
                }
                return new SyncLandOverlayPayload(dimension, List.copyOf(cells));
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
