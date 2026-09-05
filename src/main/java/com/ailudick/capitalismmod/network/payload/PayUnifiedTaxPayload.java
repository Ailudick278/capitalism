package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PayUnifiedTaxPayload(String billId) implements CustomPacketPayload {
    public static final Type<PayUnifiedTaxPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "pay_unified_tax"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PayUnifiedTaxPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, PayUnifiedTaxPayload::billId, PayUnifiedTaxPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
