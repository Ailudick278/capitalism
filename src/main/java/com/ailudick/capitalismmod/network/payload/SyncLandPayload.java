package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SyncLandPayload(boolean claimed, String id, String dimension, int chunkX, int chunkZ,
                              String ownerUuid, String purpose, String linkedBusinessId,
                              String resourceType, long resourceAmount, long taxOwed,
                              boolean trusted, boolean leased, String leaseeUuid, long leaseUntil, long leaseRent,
                              long leaseDebt, long leaseGraceUntil,
                              long taxDueAt, long taxGraceUntil,
                              List<MapCell> mapCells)
        implements CustomPacketPayload {
    public record MapCell(int chunkX, int chunkZ, boolean claimed, boolean ownedByPlayer, boolean auction) {}
    public static final Type<SyncLandPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_land"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLandPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeBoolean(value.claimed()); buf.writeUtf(value.id()); buf.writeUtf(value.dimension());
                buf.writeVarInt(value.chunkX()); buf.writeVarInt(value.chunkZ()); buf.writeUtf(value.ownerUuid());
                buf.writeUtf(value.purpose()); buf.writeUtf(value.linkedBusinessId()); buf.writeUtf(value.resourceType());
                buf.writeVarLong(value.resourceAmount()); buf.writeVarLong(value.taxOwed());
                buf.writeBoolean(value.trusted()); buf.writeBoolean(value.leased()); buf.writeUtf(value.leaseeUuid());
                buf.writeVarLong(value.leaseUntil()); buf.writeVarLong(value.leaseRent());
                buf.writeVarLong(value.leaseDebt()); buf.writeVarLong(value.leaseGraceUntil());
                buf.writeVarLong(value.taxDueAt()); buf.writeVarLong(value.taxGraceUntil());
                buf.writeVarInt(value.mapCells().size());
                value.mapCells().forEach(cell -> {
                    buf.writeVarInt(cell.chunkX()); buf.writeVarInt(cell.chunkZ());
                    buf.writeBoolean(cell.claimed()); buf.writeBoolean(cell.ownedByPlayer()); buf.writeBoolean(cell.auction());
                });
            },
            buf -> new SyncLandPayload(buf.readBoolean(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(),
                    buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarLong(), buf.readVarLong(),
                    buf.readBoolean(), buf.readBoolean(), buf.readUtf(), buf.readVarLong(), buf.readVarLong(),
                    buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), readMapCells(buf)));

    private static List<MapCell> readMapCells(RegistryFriendlyByteBuf buf) {
        int size = Math.min(buf.readVarInt(), 81);
        java.util.ArrayList<MapCell> cells = new java.util.ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            cells.add(new MapCell(buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));
        }
        return List.copyOf(cells);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
