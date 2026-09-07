package com.ailudick.capitalismmod.market;

/** Pure logistics congestion calculations shared by dispatch and tests. */
public final class LogisticsEconomics {
    private LogisticsEconomics() {}

    /** Adds 25% of base travel time for each additional capacity batch, capped at 200%. */
    public static long congestionDelay(long baseTravelTicks, int quantity, int capacity) {
        if (baseTravelTicks <= 0L || quantity <= 0 || capacity <= 0) return Math.max(1L, baseTravelTicks);
        long batches = (quantity + (long) capacity - 1L) / capacity;
        long extraBatches = Math.min(8L, Math.max(0L, batches - 1L));
        if (extraBatches == 0L) return Math.max(1L, baseTravelTicks);
        long increment = baseTravelTicks > Long.MAX_VALUE / extraBatches
                ? Long.MAX_VALUE : baseTravelTicks * extraBatches;
        long extra = increment / 4L;
        return extra > Long.MAX_VALUE - baseTravelTicks ? Long.MAX_VALUE : Math.max(1L, baseTravelTicks + extra);
    }
}
