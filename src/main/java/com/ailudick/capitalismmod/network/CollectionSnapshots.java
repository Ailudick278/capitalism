package com.ailudick.capitalismmod.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Copies on the owning thread before asynchronous encoding. Elements must be immutable. */
public final class CollectionSnapshots {
    private CollectionSnapshots() {}

    public static <K, V> Map<K, List<V>> lists(Map<K, ? extends List<V>> source) {
        Map<K, List<V>> result = new HashMap<>();
        source.forEach((key, values) -> result.put(key, List.copyOf(values)));
        return Map.copyOf(result);
    }
}
