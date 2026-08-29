package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: move {@code count} of a commodity from the backpack into the warehouse.
 */
public record WarehouseDepositPayload(int commodityIndex, int count) implements CustomPacketPayload {
    public static final Type<WarehouseDepositPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "warehouse_deposit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WarehouseDepositPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WarehouseDepositPayload::commodityIndex,
            ByteBufCodecs.VAR_INT, WarehouseDepositPayload::count,
            WarehouseDepositPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
