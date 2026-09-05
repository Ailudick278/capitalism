package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReviewTaxRefundPayload(String requestId, boolean approve, String reason) implements CustomPacketPayload {
    public static final Type<ReviewTaxRefundPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "review_tax_refund"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReviewTaxRefundPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeUtf(value.requestId(), 64); buf.writeBoolean(value.approve()); buf.writeUtf(value.reason(), 256); },
            buf -> new ReviewTaxRefundPayload(buf.readUtf(64), buf.readBoolean(), buf.readUtf(256)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
