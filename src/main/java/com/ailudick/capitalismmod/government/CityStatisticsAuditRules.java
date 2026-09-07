package com.ailudick.capitalismmod.government;

/** Pure invariants for persisted regional city indicators. */
public final class CityStatisticsAuditRules {
    private CityStatisticsAuditRules() {}

    public static boolean valid(CityStatisticsSavedData.Snapshot snapshot) {
        return snapshot != null && snapshot.day() >= 0L && snapshot.region() != null
                && !snapshot.region().isBlank() && snapshot.residents() >= 0
                && snapshot.housing() >= 0 && snapshot.school() >= 0 && snapshot.clinic() >= 0
                && snapshot.serviceScore() >= 0 && snapshot.serviceScore() <= 100
                && snapshot.unemploymentRate() >= 0 && snapshot.unemploymentRate() <= 100
                && snapshot.dailyRentPerResident() >= 0L && snapshot.treasuryMinor() >= 0L
                && snapshot.maintenanceSpentMinor() >= 0L && snapshot.maintenanceCuts() >= 0
                && snapshot.fiscalStress() >= 0 && snapshot.fiscalStress() <= 100
                && snapshot.activeProjects() >= 0;
    }
}
