package com.ailudick.capitalismmod.population;

/** Deterministic annualized demographic rates for virtual NPC households. */
public final class DemographicEconomics {
    private static final long RATE_DENOMINATOR = 1000L * 365L;
    private DemographicEconomics() {}

    public static int annualBirthRatePerThousand(int satisfaction) {
        return 8 + Math.max(0, Math.min(100, satisfaction)) / 25;
    }

    public static int annualDeathRatePerThousand(int health) {
        return Math.max(2, 12 - Math.max(0, Math.min(100, health)) / 20);
    }

    public static boolean eventFor(String householdId, long day, int ratePerThousand, int population) {
        if (householdId == null || householdId.isBlank() || day < 0L || ratePerThousand <= 0 || population <= 0) return false;
        long threshold = Math.min(RATE_DENOMINATOR, Math.max(0L, ratePerThousand) * population);
        long seed = (long) householdId.hashCode() * 1_103_515_245L + day * 2_654_435_761L;
        return Math.floorMod(seed, RATE_DENOMINATOR) < threshold;
    }
}
