package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Server -> Client: sync the player's warehouse inventory (item id -> count).
 */
public record SyncWarehousePayload(Map<String, Integer> storage, String ownerKey, Map<String, String> owners) implements CustomPacketPayload {
    public SyncWarehousePayload(Map<String, Integer> storage) {
        this(storage, "", Map.of());
    }
    public static final Type<SyncWarehousePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_warehouse"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncWarehousePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), SyncWarehousePayload::storage,
            ByteBufCodecs.STRING_UTF8, SyncWarehousePayload::ownerKey,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8), SyncWarehousePayload::owners,
            SyncWarehousePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
