package com.ailudick.capitalismmod.market;

import com.ailudick.capitalismmod.data.CapitalismData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The list of tradable commodities on the exchange, loaded from data-driven config.
 * Commodities are keyed by their item id (e.g. "minecraft:diamond") for persistence.
 */
public final class Commodities {
    public static final List<ItemStack> ALL = CapitalismData.getCommodities();

    private Commodities() {
    }

    public static ItemStack get(int index) {
        return ALL.get(index);
    }

    public static boolean isValid(int index) {
        return index >= 0 && index < ALL.size();
    }

    /** The item registry id used as the persistence key, e.g. "minecraft:diamond". */
    public static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** Initial (fundamental) price per unit in USD, from config; 0 if unconfigured. */
    public static long initialPrice(String itemId) {
        return CapitalismData.getCommodityPrices().getOrDefault(itemId, 0L);
    }

    public static long initialPriceOf(ItemStack stack) {
        return initialPrice(id(stack));
    }

    /** The commodity ItemStack for a persistence key, or {@code null} if unknown. */
    public static ItemStack byId(String itemId) {
        for (ItemStack stack : ALL) {
            if (id(stack).equals(itemId)) {
                return stack;
            }
        }
        return null;
    }
}
