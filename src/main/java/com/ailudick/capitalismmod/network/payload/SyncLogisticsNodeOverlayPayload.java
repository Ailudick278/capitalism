package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Explored logistics facilities shown on the world map. */
public record SyncLogisticsNodeOverlayPayload(String dimension, List<Node> nodes)
        implements CustomPacketPayload {
    public record Node(int chunkX, int chunkZ, String facility) {}

    public static final Type<SyncLogisticsNodeOverlayPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_logistics_nodes"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLogisticsNodeOverlayPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.dimension());
                buf.writeVarInt(value.nodes().size());
                for (Node node : value.nodes()) {
                    buf.writeVarInt(node.chunkX());
                    buf.writeVarInt(node.chunkZ());
                    buf.writeUtf(node.facility(), 64);
                }
            },
            buf -> {
                String dimension = buf.readUtf();
                int size = Math.min(buf.readVarInt(), 4096);
                List<Node> nodes = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    nodes.add(new Node(buf.readVarInt(), buf.readVarInt(), buf.readUtf(64)));
                }
                return new SyncLogisticsNodeOverlayPayload(dimension, List.copyOf(nodes));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
