package com.ailudick.capitalismmod.population;

/** Persistent virtual household used by the first population simulation layer. */
public record Household(String id, String region, int size, int workingAge,
                        long cashMinor, long dailyNeedMinor, int satisfaction,
                        long lastSettlementDay, long lastMigrationDay) {
    public Household(String id, String region, int size, int workingAge, long cashMinor,
                     long dailyNeedMinor, int satisfaction, long lastSettlementDay) {
        this(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction, lastSettlementDay, -1L);
    }
    public Household {
        id = id == null ? "" : id.trim(); region = region == null || region.isBlank() ? "spawn" : region.trim();
        if (id.isBlank() || size <= 0 || workingAge < 0 || workingAge > size || cashMinor < 0L
                || dailyNeedMinor < 0L || satisfaction < 0 || satisfaction > 100 || lastSettlementDay < -1L || lastMigrationDay < -1L) {
            throw new IllegalArgumentException("Invalid household");
        }
    }

    public Household withSettlement(long day, long cash, int satisfaction, String nextRegion) {
        return new Household(id, nextRegion, size, workingAge, Math.max(0L, cash), dailyNeedMinor,
                Math.max(0, Math.min(100, satisfaction)), day, lastMigrationDay);
    }
    public Household withCash(long cash) { return new Household(id, region, size, workingAge, Math.max(0L, cash), dailyNeedMinor, satisfaction, lastSettlementDay, lastMigrationDay); }
    public Household withRegion(String nextRegion, long day) { return new Household(id, nextRegion, size, workingAge, cashMinor, dailyNeedMinor, satisfaction, lastSettlementDay, day); }
}
