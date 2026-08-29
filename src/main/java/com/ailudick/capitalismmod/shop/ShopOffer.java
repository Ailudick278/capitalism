package com.ailudick.capitalismmod.shop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * A single offer in a shop: an item sold for a fixed price in a specific currency.
 */
public record ShopOffer(ItemStack item, int price, String currencyId) {

    // StreamCodec for networking. Must use RegistryFriendlyByteBuf because ItemStack's codec is registry-aware.
    public static final StreamCodec<RegistryFriendlyByteBuf, ShopOffer> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, ShopOffer::item,
            ByteBufCodecs.VAR_INT, ShopOffer::price,
            ByteBufCodecs.STRING_UTF8, ShopOffer::currencyId,
            ShopOffer::new);
}
