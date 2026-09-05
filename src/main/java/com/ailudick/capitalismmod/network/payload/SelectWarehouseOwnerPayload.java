package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectWarehouseOwnerPayload(String ownerKey) implements CustomPacketPayload {
    public static final Type<SelectWarehouseOwnerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "select_warehouse_owner"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectWarehouseOwnerPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SelectWarehouseOwnerPayload::ownerKey,
                    SelectWarehouseOwnerPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
