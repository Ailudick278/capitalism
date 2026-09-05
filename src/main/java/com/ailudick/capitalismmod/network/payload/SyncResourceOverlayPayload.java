package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Explored natural-resource markers for the world map. */
public record SyncResourceOverlayPayload(String dimension, List<OilField> oilFields)
        implements CustomPacketPayload {
    public record OilField(int chunkX, int chunkZ, long remainingReserve, long initialReserve) {}

    public static final Type<SyncResourceOverlayPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_resource_overlay"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncResourceOverlayPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.dimension());
                buf.writeVarInt(value.oilFields().size());
                for (OilField field : value.oilFields()) {
                    buf.writeVarInt(field.chunkX());
                    buf.writeVarInt(field.chunkZ());
                    buf.writeVarLong(field.remainingReserve());
                    buf.writeVarLong(field.initialReserve());
                }
            },
            buf -> {
                String dimension = buf.readUtf();
                int size = Math.min(buf.readVarInt(), 4096);
                List<OilField> fields = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    fields.add(new OilField(buf.readVarInt(), buf.readVarInt(),
                            buf.readVarLong(), buf.readVarLong()));
                }
                return new SyncResourceOverlayPayload(dimension, List.copyOf(fields));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
