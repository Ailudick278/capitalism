package com.ailudick.capitalismmod.company;

import java.util.Map;

/**
 * The data-driven production recipe for one industry.
 *
 * @param id      industry id (see {@link CompanyTypes})
 * @param inputs  item id -> quantity consumed per level per tick
 * @param outputs item id -> quantity produced per level per tick
 * @param income  USD income per level per tick (finance uses a special formula)
 */
public record IndustrySpec(String id, Map<String, Integer> inputs, Map<String, Integer> outputs, long income) {
}
