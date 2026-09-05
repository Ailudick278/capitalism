package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeclareTaxPayload(String billId) implements CustomPacketPayload {
    public static final Type<DeclareTaxPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "declare_tax"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeclareTaxPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, DeclareTaxPayload::billId, DeclareTaxPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
