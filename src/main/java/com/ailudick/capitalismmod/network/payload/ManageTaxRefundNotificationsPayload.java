package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ManageTaxRefundNotificationsPayload(String action, String requestId) implements CustomPacketPayload {
    public static final Type<ManageTaxRefundNotificationsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "manage_tax_refund_notifications"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ManageTaxRefundNotificationsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeUtf(value.action(), 32); buf.writeUtf(value.requestId(), 64); },
            buf -> new ManageTaxRefundNotificationsPayload(buf.readUtf(32), buf.readUtf(64)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
