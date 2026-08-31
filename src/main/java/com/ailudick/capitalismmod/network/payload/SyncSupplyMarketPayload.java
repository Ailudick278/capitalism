package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.supply.PurchaseOrder;
import com.ailudick.capitalismmod.supply.SupplyOffer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server -> Client: sync supplier offers and the player's own pending orders.
 */
public record SyncSupplyMarketPayload(List<SupplyOffer> offers, List<PurchaseOrder> orders) implements CustomPacketPayload {
    public static final Type<SyncSupplyMarketPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_supply_market"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSupplyMarketPayload> STREAM_CODEC = StreamCodec.composite(
            SupplyOffer.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncSupplyMarketPayload::offers,
            PurchaseOrder.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncSupplyMarketPayload::orders,
            SyncSupplyMarketPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
