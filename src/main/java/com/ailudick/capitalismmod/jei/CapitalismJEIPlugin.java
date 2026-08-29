package com.ailudick.capitalismmod.jei;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.data.CapitalismData;
import com.ailudick.capitalismmod.shop.ShopOffer;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI integration: shows shop offer prices in JEI's item viewer.
 * Loaded only when JEI is present (compileOnly dependency).
 */
@JeiPlugin
public class CapitalismJEIPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CapitalismMod.MODID, "jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (ShopOffer offer : CapitalismData.getShopOffers()) {
            registration.addItemStackInfo(offer.item(),
                    Component.translatable("jei.capitalismmod.shop_offer", offer.price(), offer.currencyId()));
        }
    }
}
