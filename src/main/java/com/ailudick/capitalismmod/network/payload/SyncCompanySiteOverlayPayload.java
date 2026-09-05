package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Explored company operating sites shown as a public map overlay. */
public record SyncCompanySiteOverlayPayload(String dimension, List<Site> sites)
        implements CustomPacketPayload {
    public record Site(int chunkX, int chunkZ, String companyName, String companyType) {}

    public static final Type<SyncCompanySiteOverlayPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_company_sites"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCompanySiteOverlayPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.dimension());
                buf.writeVarInt(value.sites().size());
                for (Site site : value.sites()) {
                    buf.writeVarInt(site.chunkX());
                    buf.writeVarInt(site.chunkZ());
                    buf.writeUtf(site.companyName(), 64);
                    buf.writeUtf(site.companyType(), 64);
                }
            },
            buf -> {
                String dimension = buf.readUtf();
                int size = Math.min(buf.readVarInt(), 4096);
                List<Site> sites = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    sites.add(new Site(buf.readVarInt(), buf.readVarInt(), buf.readUtf(64), buf.readUtf(64)));
                }
                return new SyncCompanySiteOverlayPayload(dimension, List.copyOf(sites));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
