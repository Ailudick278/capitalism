package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.company.Company;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server -> Client: sync the player's companies and their listed status for the securities commission GUI.
 */
public record SyncSecuritiesPayload(Map<String, Company> companies, List<String> listed) implements CustomPacketPayload {
    public static final Type<SyncSecuritiesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_securities"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSecuritiesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.fromCodec(Company.CODEC)), SyncSecuritiesPayload::companies,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncSecuritiesPayload::listed,
            SyncSecuritiesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
