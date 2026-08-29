package com.ailudick.capitalismmod.stock;

import com.ailudick.capitalismmod.data.CapitalismData;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The list of tradable stocks, loaded from data-driven config.
 */
public final class Stocks {
    public static final List<Stock> ALL = CapitalismData.getStocks();

    private static final Map<String, Stock> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(Stock::id, Function.identity()));

    private Stocks() {
    }

    public static boolean exists(String id) {
        return BY_ID.containsKey(id);
    }

    public static Stock byId(String id) {
        return BY_ID.get(id);
    }
}
