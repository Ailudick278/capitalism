package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record SyncPersonalAssetsPayload(Map<String, Long> assets) implements CustomPacketPayload {
    public static final Type<SyncPersonalAssetsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_personal_assets"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPersonalAssetsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG),
            SyncPersonalAssetsPayload::assets, SyncPersonalAssetsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
