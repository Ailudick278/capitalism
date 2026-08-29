package com.ailudick.capitalismmod.stock;

/**
 * A tradable stock.
 *
 * @param initialPrice initial price per share, in USD
 */
public record Stock(String id, String nameKey, long initialPrice) {
}
