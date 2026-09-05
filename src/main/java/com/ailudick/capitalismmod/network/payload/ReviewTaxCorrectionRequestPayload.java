package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReviewTaxCorrectionRequestPayload(String id, boolean approve, String reason) implements CustomPacketPayload {
    public static final Type<ReviewTaxCorrectionRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "review_tax_correction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReviewTaxCorrectionRequestPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ReviewTaxCorrectionRequestPayload::id, ByteBufCodecs.BOOL, ReviewTaxCorrectionRequestPayload::approve, ByteBufCodecs.STRING_UTF8, ReviewTaxCorrectionRequestPayload::reason, ReviewTaxCorrectionRequestPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
