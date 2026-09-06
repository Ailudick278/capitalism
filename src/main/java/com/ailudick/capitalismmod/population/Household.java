package com.ailudick.capitalismmod.population;

/** Persistent virtual household used by the first population simulation layer. */
public record Household(String id, String region, int size, int workingAge,
                        long cashMinor, long dailyNeedMinor, int satisfaction,
                        long lastSettlementDay, long lastMigrationDay, int health, int education,
                        int unemploymentDays, int employmentDays) {
    public Household(String id, String region, int size, int workingAge, long cashMinor,
                     long dailyNeedMinor, int satisfaction, long lastSettlementDay) {
        this(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction, lastSettlementDay, -1L, 70, 0, 0, 0);
    }
    public Household(String id, String region, int size, int workingAge, long cashMinor,
                     long dailyNeedMinor, int satisfaction, long lastSettlementDay, long lastMigrationDay) {
        this(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction, lastSettlementDay, lastMigrationDay, 70, 0, 0, 0);
    }
    public Household(String id, String region, int size, int workingAge, long cashMinor,
                     long dailyNeedMinor, int satisfaction, long lastSettlementDay,
                     long lastMigrationDay, int health, int education) {
        this(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction,
                lastSettlementDay, lastMigrationDay, health, education, 0);
    }
    public Household(String id, String region, int size, int workingAge, long cashMinor,
                     long dailyNeedMinor, int satisfaction, long lastSettlementDay,
                     long lastMigrationDay, int health, int education, int unemploymentDays) {
        this(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction,
                lastSettlementDay, lastMigrationDay, health, education, unemploymentDays, 0);
    }
    public Household {
        id = id == null ? "" : id.trim(); region = region == null || region.isBlank() ? "spawn" : region.trim();
        if (id.isBlank() || size <= 0 || workingAge < 0 || workingAge > size || cashMinor < 0L
                || dailyNeedMinor < 0L || satisfaction < 0 || satisfaction > 100 || lastSettlementDay < -1L || lastMigrationDay < -1L
                || health < 0 || health > 100 || education < 0 || education > 100
                || unemploymentDays < 0 || unemploymentDays > 10000
                || employmentDays < 0 || employmentDays > 10000) {
            throw new IllegalArgumentException("Invalid household");
        }
    }

    public Household withSettlement(long day, long cash, int satisfaction, String nextRegion) {
        return new Household(id, nextRegion, size, workingAge, Math.max(0L, cash), dailyNeedMinor,
                Math.max(0, Math.min(100, satisfaction)), day, lastMigrationDay, health, education, unemploymentDays, employmentDays);
    }
    public Household withCash(long cash) { return new Household(id, region, size, workingAge, Math.max(0L, cash), dailyNeedMinor, satisfaction, lastSettlementDay, lastMigrationDay, health, education, unemploymentDays, employmentDays); }
    public Household withRegion(String nextRegion, long day) { return new Household(id, nextRegion, size, workingAge, cashMinor, dailyNeedMinor, satisfaction, lastSettlementDay, day, health, education, unemploymentDays, employmentDays); }
    public Household withHumanCapital(int nextHealth, int nextEducation) {
        return new Household(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction,
                lastSettlementDay, lastMigrationDay, nextHealth, nextEducation, unemploymentDays, employmentDays);
    }
    public Household withEmploymentState(boolean employed) {
        int days = employed ? 0 : Math.min(10000, unemploymentDays + (workingAge > 0 ? 1 : 0));
        int worked = employed ? Math.min(10000, employmentDays + 1) : employmentDays;
        return new Household(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction,
                lastSettlementDay, lastMigrationDay, health, education, days, worked);
    }
    public Household withDemographics(int nextSize, int nextWorkingAge) {
        int safeSize = Math.max(1, nextSize);
        return new Household(id, region, safeSize, Math.max(0, Math.min(safeSize, nextWorkingAge)), cashMinor,
                dailyNeedMinor, satisfaction, lastSettlementDay, lastMigrationDay, health, education,
                unemploymentDays, employmentDays);
    }
}
