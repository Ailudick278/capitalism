package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: pay a company's accrued corporate income tax.
 */
public record PayTaxPayload(String companyName) implements CustomPacketPayload {
    public static final Type<PayTaxPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "pay_tax"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PayTaxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PayTaxPayload::companyName,
            PayTaxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
