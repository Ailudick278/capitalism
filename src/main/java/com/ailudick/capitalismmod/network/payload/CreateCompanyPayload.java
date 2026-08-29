package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: create a company with the given type and name.
 */
public record CreateCompanyPayload(String companyType, String name) implements CustomPacketPayload {
    public static final Type<CreateCompanyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "create_company"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CreateCompanyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CreateCompanyPayload::companyType,
            ByteBufCodecs.STRING_UTF8, CreateCompanyPayload::name,
            CreateCompanyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
