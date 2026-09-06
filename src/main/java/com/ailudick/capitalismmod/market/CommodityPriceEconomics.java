package com.ailudick.capitalismmod.market;

/** Pure daily commodity price movement from fundamentals and excess pressure. */
public final class CommodityPriceEconomics {
    private CommodityPriceEconomics() {}

    /** Calculates the unconstrained close: demand raises price, excess supply lowers it. */
    public static long nextPrice(long oldPrice, long fundamental, long netVolume, long excessSupply) {
        if (oldPrice <= 0L) oldPrice = Math.max(1L, fundamental);
        long reversion = (fundamental - oldPrice) / 10L;
        long pressure;
        try {
            pressure = Math.subtractExact(netVolume, excessSupply);
        } catch (ArithmeticException exception) {
            pressure = netVolume >= 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
        long movement = pressure / 10L;
        long candidate;
        try {
            candidate = Math.addExact(Math.addExact(oldPrice, reversion), movement);
        } catch (ArithmeticException exception) {
            candidate = movement < 0L ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return Math.max(1L, candidate);
    }
}
