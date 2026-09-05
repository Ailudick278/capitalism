package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record SyncWorldMapTilesPayload(List<Tile> tiles) implements CustomPacketPayload {
    public record Tile(int chunkX, int chunkZ, int[] colors) {}

    public static final Type<SyncWorldMapTilesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_world_map_tiles"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncWorldMapTilesPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.tiles().size());
                for (Tile tile : value.tiles()) {
                    buf.writeVarInt(tile.chunkX());
                    buf.writeVarInt(tile.chunkZ());
                    for (int color : tile.colors()) buf.writeInt(color);
                }
            },
            buf -> {
                int size = Math.min(buf.readVarInt(), 81);
                List<Tile> tiles = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    int chunkX = buf.readVarInt();
                    int chunkZ = buf.readVarInt();
                    int[] colors = new int[256];
                    for (int j = 0; j < colors.length; j++) colors[j] = buf.readInt();
                    tiles.add(new Tile(chunkX, chunkZ, colors));
                }
                return new SyncWorldMapTilesPayload(List.copyOf(tiles));
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
