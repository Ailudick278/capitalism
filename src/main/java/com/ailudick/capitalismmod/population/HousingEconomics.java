package com.ailudick.capitalismmod.population;

/** Pure housing and commute arithmetic used by the regional simulation. */
public final class HousingEconomics {
    private HousingEconomics() {}

    public static long dailyRent(long baseRentPerResident, int residents, int housingUnits) {
        if (baseRentPerResident <= 0L || residents <= 0) return 0L;
        int capacity = housingUnits <= 0 ? residents : Math.max(1, housingUnits * 4);
        if (residents <= capacity) return baseRentPerResident;
        long excessRate = Math.min(200L, ((long) (residents - capacity) * 100L) / capacity);
        long premium = baseRentPerResident > Long.MAX_VALUE / excessRate
                ? Long.MAX_VALUE : baseRentPerResident * excessRate / 100L;
        return premium > Long.MAX_VALUE - baseRentPerResident
                ? Long.MAX_VALUE : baseRentPerResident + premium;
    }

    public static long commuteCost(long dailyNeedPerResident, int householdSize, int regionDistance) {
        if (dailyNeedPerResident <= 0L || householdSize <= 0 || regionDistance <= 0) return 0L;
        long people;
        try { people = Math.multiplyExact(dailyNeedPerResident, householdSize); }
        catch (ArithmeticException e) { return Long.MAX_VALUE; }
        long distance = Math.min(365L, regionDistance);
        return people > Long.MAX_VALUE / distance ? Long.MAX_VALUE : people * distance / 10L;
    }
}
