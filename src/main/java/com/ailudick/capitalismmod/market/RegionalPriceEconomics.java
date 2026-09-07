package com.ailudick.capitalismmod.market;

/** Regional price adjustment caused by logistics access and transport friction. */
public final class RegionalPriceEconomics {
    private RegionalPriceEconomics() {}

    /** Isolated regions pay up to a 20% logistics premium; fully connected regions pay none. */
    public static long withLogisticsPremium(long globalPrice, int accessScore) {
        long price = Math.max(1L, globalPrice);
        int access = Math.max(0, Math.min(100, accessScore));
        long premiumBps = (100L - access) * 20L;
        if (price > (Long.MAX_VALUE - premiumBps) / 10_000L) return Long.MAX_VALUE;
        return Math.max(1L, price + price * premiumBps / 10_000L);
    }
}
