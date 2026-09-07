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

    /** Converts active cargo load into a bounded regional traffic score. */
    public static int congestionScore(long load, long capacity) {
        if (load <= 0L || capacity <= 0L) return 0;
        long scaled = load > Long.MAX_VALUE / 100L ? Long.MAX_VALUE : load * 100L;
        return (int) Math.min(100L, scaled / capacity);
    }

    /** Congestion lowers locational value modestly; it never makes a location worthless. */
    public static double landValueMultiplier(int congestionScore) {
        int score = Math.max(0, Math.min(100, congestionScore));
        return 1.0 - score / 666.6666666667;
    }
}
