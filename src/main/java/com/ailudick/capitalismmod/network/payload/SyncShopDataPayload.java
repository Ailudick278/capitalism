package com.ailudick.capitalismmod.network.payload;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.shop.ShopOffer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server -> Client: sync the list of offers available in the shop being viewed.
 */
public record SyncShopDataPayload(List<ShopOffer> offers) implements CustomPacketPayload {
    public static final Type<SyncShopDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "sync_shop_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncShopDataPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ShopOffer.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncShopDataPayload::offers,
                    SyncShopDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
