package com.ailudick.capitalismmod.shop;

import com.ailudick.capitalismmod.data.CapitalismData;

import java.util.List;

/**
 * Default stock used by every shop, loaded from data-driven config.
 */
public final class ShopOffers {
    private ShopOffers() {
    }

    public static List<ShopOffer> defaultOffers() {
        return CapitalismData.getShopOffers();
    }
}
