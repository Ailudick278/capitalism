package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> Server: open a long or short futures position.
 *
 * @param longSide {@code true} = long, {@code false} = short
 */
public record OpenFuturesPositionPayload(int commodityIndex, int quantity, boolean longSide) implements CustomPacketPayload {
    public static final Type<OpenFuturesPositionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "open_futures_position"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenFuturesPositionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenFuturesPositionPayload::commodityIndex,
            ByteBufCodecs.VAR_INT, OpenFuturesPositionPayload::quantity,
            ByteBufCodecs.BOOL, OpenFuturesPositionPayload::longSide,
            OpenFuturesPositionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
