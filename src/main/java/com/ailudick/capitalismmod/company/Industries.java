package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.data.CapitalismData;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The data-driven production recipes for all industries, loaded from config.
 */
public final class Industries {
    public static final List<IndustrySpec> ALL = CapitalismData.getIndustries();

    private static final Map<String, IndustrySpec> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(IndustrySpec::id, Function.identity()));

    private Industries() {
    }

    /** The production recipe for an industry, or {@code null} if not configured. */
    public static IndustrySpec byId(String id) {
        return BY_ID.get(id);
    }
}
