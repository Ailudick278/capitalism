package com.ailudick.capitalismmod.network.payload;
import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
public record SyncLandLogsPayload(List<String> logs) implements CustomPacketPayload {
    public static final Type<SyncLandLogsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_land_logs"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLandLogsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeVarInt(value.logs().size()); value.logs().forEach(buf::writeUtf); },
            buf -> { int n = Math.min(buf.readVarInt(), 32); java.util.ArrayList<String> logs = new java.util.ArrayList<>(n);
                for (int i = 0; i < n; i++) logs.add(buf.readUtf(256)); return new SyncLandLogsPayload(List.copyOf(logs)); });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
