package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: withdraw a company's entire USD treasury to the founder.
 */
public record WithdrawCompanyPayload(String companyName) implements CustomPacketPayload {
    public static final Type<WithdrawCompanyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "withdraw_company"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WithdrawCompanyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WithdrawCompanyPayload::companyName,
            WithdrawCompanyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
