package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.tax.TaxCorrectionRequestSavedData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

public record SyncTaxCorrectionRequestsPayload(List<TaxCorrectionRequestSavedData.Request> requests) implements CustomPacketPayload {
    public static final Type<SyncTaxCorrectionRequestsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_tax_correction_requests"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTaxCorrectionRequestsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { int size = Math.min(value.requests().size(), 128); buf.writeVarInt(size); for (var r : value.requests().subList(0, size)) { buf.writeUtf(r.id(), 64); buf.writeUtf(r.businessId(), 128); buf.writeVarLong(r.periodEnd()); buf.writeVarLong(r.revenue()); buf.writeVarLong(r.expenses()); buf.writeUtf(r.reason(), 256); buf.writeUUID(r.applicant()); buf.writeVarLong(r.createdAt()); buf.writeUtf(r.status(), 16); buf.writeUtf(r.reviewer(), 64); buf.writeVarLong(r.reviewedAt()); buf.writeUtf(r.reviewReason(), 256); } },
            buf -> { int size = Math.min(buf.readVarInt(), 128); List<TaxCorrectionRequestSavedData.Request> result = new ArrayList<>(size); for (int i = 0; i < size; i++) result.add(new TaxCorrectionRequestSavedData.Request(buf.readUtf(64), buf.readUtf(128), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readUtf(256), buf.readUUID(), buf.readVarLong(), buf.readUtf(16), buf.readUtf(64), buf.readVarLong(), buf.readUtf(256))); return new SyncTaxCorrectionRequestsPayload(List.copyOf(result)); });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
