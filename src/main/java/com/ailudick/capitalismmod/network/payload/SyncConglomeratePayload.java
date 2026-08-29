package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.company.Company;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Server -> Client: sync the player's conglomerate (name + companies).
 */
public record SyncConglomeratePayload(String name, Map<String, Company> companies) implements CustomPacketPayload {
    public static final Type<SyncConglomeratePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_conglomerate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncConglomeratePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SyncConglomeratePayload::name,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.fromCodec(Company.CODEC)), SyncConglomeratePayload::companies,
            SyncConglomeratePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
