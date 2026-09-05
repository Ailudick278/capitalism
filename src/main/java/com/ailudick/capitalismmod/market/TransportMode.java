package com.ailudick.capitalismmod.market;

/** Automatic transport choices used by regional procurement. */
public enum TransportMode {
    ROAD("road", 1, 1, 64, "capitalismmod:gasoline"),
    RAIL("rail", 3, 5, 256, "capitalismmod:diesel"),
    SEA("sea", 2, 5, 512, "capitalismmod:fuel_oil");

    private final String id;
    private final long timeNumerator;
    private final long timeDenominator;
    private final int capacity;
    private final String fuelItemId;

    TransportMode(String id, long timeNumerator, long timeDenominator, int capacity, String fuelItemId) {
        this.id = id;
        this.timeNumerator = timeNumerator;
        this.timeDenominator = timeDenominator;
        this.capacity = capacity;
        this.fuelItemId = fuelItemId;
    }

    public String id() {
        return id;
    }

    public int capacity() {
        return capacity;
    }

    public String fuelItemId() {
        return fuelItemId;
    }

    /** Coarse planning estimate, not an immediate inventory charge. */
    public int estimatedFuelUnits(int quantity, long regionDistance) {
        if (quantity <= 0 || regionDistance <= 0) return 0;
        long divisor = this == SEA ? 128L : this == RAIL ? 192L : 256L;
        long estimate = ((long) quantity * regionDistance + divisor - 1L) / divisor;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, estimate));
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
