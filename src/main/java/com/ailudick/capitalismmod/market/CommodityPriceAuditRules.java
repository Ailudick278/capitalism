package com.ailudick.capitalismmod.market;

/** Pure invariants for persisted commodity prices and OHLC history. */
public final class CommodityPriceAuditRules {
    private CommodityPriceAuditRules() {}

    public static boolean validPrice(String itemId, long price, long fundamental, long previousClose) {
        return itemId != null && !itemId.isBlank() && price > 0L && fundamental > 0L && previousClose > 0L;
    }

    public static boolean validCandle(long open, long high, long low, long close) {
        return open > 0L && high > 0L && low > 0L && close > 0L
                && high >= Math.max(open, close) && low <= Math.min(open, close) && high >= low;
    }
}
