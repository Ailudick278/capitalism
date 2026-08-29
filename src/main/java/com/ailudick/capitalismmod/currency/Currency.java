package com.ailudick.capitalismmod.currency;

/**
 * A currency type in the economy.
 *
 * @param id        unique id, also used as the wallet balance key and part of translation keys
 * @param nameKey   translation key for the display name
 * @param baseValue value relative to the base currency (copper = 1), used for exchange-rate conversion
 */
public record Currency(String id, String nameKey, long baseValue) {
}
