package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.init.ModItems;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;
import java.util.Optional;

/**
 * Replaces emeralds in vanilla villager trades with the mod's currency item.
 *
 * <p>One emerald maps to one $50 bill (USD_50), matching the emerald's 50-dollar
 * initial price on the commodity exchange.
 */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public class VillagerTradeHandler {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        for (List<ItemListing> listings : event.getTrades().values()) {
            for (int i = 0; i < listings.size(); i++) {
                listings.set(i, wrap(listings.get(i)));
            }
        }
    }

    /** Wraps an original trade so its generated offer converts emeralds to currency. */
    private static ItemListing wrap(ItemListing original) {
        return (trader, random) -> convert(original.getOffer(trader, random));
    }

    private static MerchantOffer convert(MerchantOffer offer) {
        ItemCost costA = convertCost(offer.getItemCostA());
        Optional<ItemCost> costB = offer.getItemCostB().map(VillagerTradeHandler::convertCost);
        ItemStack result = convertStack(offer.getResult());
        return new MerchantOffer(costA, costB, result, offer.getMaxUses(), offer.getXp(), offer.getPriceMultiplier());
    }

    private static ItemCost convertCost(ItemCost cost) {
        if (cost.itemStack().is(Items.EMERALD)) {
            return new ItemCost(currency(), cost.count());
        }
        return cost;
    }

    private static ItemStack convertStack(ItemStack stack) {
        if (stack.is(Items.EMERALD)) {
            return new ItemStack(currency(), stack.getCount());
        }
        return stack;
    }

    private static Item currency() {
        return ModItems.USD_50.get();
    }
}
