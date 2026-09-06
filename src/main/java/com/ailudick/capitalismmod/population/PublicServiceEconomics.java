package com.ailudick.capitalismmod.population;

/** Pure public-service coverage arithmetic for deterministic tests and reports. */
public final class PublicServiceEconomics {
    private PublicServiceEconomics() {}

    public static int score(int residents, int housingUnits, int schoolUnits, int clinicUnits) {
        if (residents <= 0) return 100;
        int housing = housingUnits <= 0 ? 100 : coverage(housingUnits * 4, residents);
        int school = coverage(schoolUnits * 10, residents);
        int clinic = coverage(clinicUnits * 10, residents);
        return Math.max(0, Math.min(100, housing * 40 / 100 + school * 30 / 100 + clinic * 30 / 100));
    }

    private static int coverage(int capacity, int residents) {
        return Math.min(100, Math.max(0, capacity * 100 / residents));
    }
}
