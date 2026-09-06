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

    /** One-time relocation cost: two weeks of living costs, one week of rent, plus access friction. */
    public static long migrationCost(long dailyNeedPerResident, int householdSize,
                                     long destinationRentPerResident, long friction) {
        if (dailyNeedPerResident <= 0L || householdSize <= 0) return Math.max(0L, friction);
        long dailyNeed = multiply(dailyNeedPerResident, householdSize);
        long weeklyRent = multiply(destinationRentPerResident, householdSize);
        long livingPart = multiply(dailyNeed, 14L);
        long rentPart = multiply(weeklyRent, 7L);
        return add(add(livingPart, rentPart), Math.max(0L, friction));
    }

    private static long multiply(long left, long right) {
        try { return Math.multiplyExact(left, right); }
        catch (ArithmeticException e) { return Long.MAX_VALUE; }
    }

    private static long add(long left, long right) {
        try { return Math.addExact(left, right); }
        catch (ArithmeticException e) { return Long.MAX_VALUE; }
    }
}
