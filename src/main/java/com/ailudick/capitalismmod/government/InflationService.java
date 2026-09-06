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
        for (ItemStack stack : Commodities.ALL) {
            String id = Commodities.id(stack).toLowerCase(Locale.ROOT);
            if (containsAny(id, "food", "bread", "wheat", "potato", "carrot", "apple", "beef", "pork",
                    "coal", "fuel", "diesel", "planks", "wood", "furniture")) basket.add(Commodities.id(stack));
        }
        Map<String, Long> current = commodities.prices();
        Map<String, Long> base = new java.util.HashMap<>();
        for (String id : basket) base.put(id, commodities.fundamental(id));
        int index = InflationEconomics.weightedIndex(current, base, new ArrayList<>(basket));
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
}
