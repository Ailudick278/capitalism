package com.ailudick.capitalismmod.government;

import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

/** Builds a transparent household-consumption price basket from market commodities. */
public final class InflationService {
    private InflationService() {}

    public static InflationSavedData.Snapshot settleDaily(MinecraftServer server, long day) {
        InflationSavedData data = InflationSavedData.get(server);
        InflationSavedData.Snapshot latest = data.latest();
        if (latest != null && latest.day() >= day) return latest;
        CommoditySavedData commodities = CommoditySavedData.get(server);
        Set<String> basket = new LinkedHashSet<>();
        Set<String> food = new LinkedHashSet<>();
        Set<String> energy = new LinkedHashSet<>();
        Set<String> living = new LinkedHashSet<>();
        Map<String, Integer> weights = new java.util.HashMap<>();
        for (ItemStack stack : Commodities.ALL) {
            String id = Commodities.id(stack).toLowerCase(Locale.ROOT);
            String original = Commodities.id(stack);
            if (containsAny(id, "food", "bread", "wheat", "potato", "carrot", "apple", "beef", "pork")) {
                basket.add(original); food.add(original);
            } else if (containsAny(id, "coal", "fuel", "diesel")) {
                basket.add(original); energy.add(original);
            } else if (containsAny(id, "planks", "wood", "furniture")) {
                basket.add(original); living.add(original);
            }
        }
        assignCategoryWeights(weights, food, 5000);
        assignCategoryWeights(weights, energy, 3000);
        assignCategoryWeights(weights, living, 2000);
        Map<String, Long> current = commodities.prices();
        Map<String, Long> base = new java.util.HashMap<>();
        for (String id : basket) base.put(id, commodities.fundamental(id));
        int index = InflationEconomics.weightedIndex(current, base, weights);
        int previous = latest == null ? index : latest.indexBps();
        int change = index - previous;
        InflationSavedData.Snapshot snapshot = new InflationSavedData.Snapshot(day, index, change);
        data.record(snapshot);
        return snapshot;
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private static void assignCategoryWeights(Map<String, Integer> weights, Set<String> items, int categoryWeight) {
        if (items.isEmpty()) return;
        int perItem = Math.max(1, categoryWeight / items.size());
        items.forEach(item -> weights.put(item, perItem));
    }
}
