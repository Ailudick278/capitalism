package com.ailudick.capitalismmod.market;

/** Automatic transport choices used by regional procurement. */
public enum TransportMode {
    ROAD("road", 1, 1, 64),
    RAIL("rail", 3, 5, 256),
    SEA("sea", 2, 5, 512);

    private final String id;
    private final long timeNumerator;
    private final long timeDenominator;
    private final int capacity;

    TransportMode(String id, long timeNumerator, long timeDenominator, int capacity) {
        this.id = id;
        this.timeNumerator = timeNumerator;
        this.timeDenominator = timeDenominator;
        this.capacity = capacity;
    }

    public String id() {
        return id;
    }

    public int capacity() {
        return capacity;
    }

    public long travelTicks(long baseTicks, long regionDistance) {
        try {
            long raw = Math.multiplyExact(baseTicks, regionDistance);
            return Math.max(1L, Math.multiplyExact(raw, timeNumerator) / timeDenominator);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }

    public static TransportMode forDistance(long regionDistance) {
        if (regionDistance <= 2) {
            return ROAD;
        }
        if (regionDistance <= 8) {
            return RAIL;
        }
        return SEA;
    }

    public static TransportMode parse(String id) {
        for (TransportMode mode : values()) {
            if (mode.id.equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return ROAD;
    }
}
