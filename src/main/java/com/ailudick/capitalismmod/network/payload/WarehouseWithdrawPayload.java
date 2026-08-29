package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: move {@code count} of a commodity from the warehouse into the backpack.
 */
public record WarehouseWithdrawPayload(int commodityIndex, int count) implements CustomPacketPayload {
    public static final Type<WarehouseWithdrawPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "warehouse_withdraw"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WarehouseWithdrawPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WarehouseWithdrawPayload::commodityIndex,
            ByteBufCodecs.VAR_INT, WarehouseWithdrawPayload::count,
            WarehouseWithdrawPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
