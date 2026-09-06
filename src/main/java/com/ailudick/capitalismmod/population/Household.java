package com.ailudick.capitalismmod.population;

/** Persistent virtual household used by the first population simulation layer. */
public record Household(String id, String region, int size, int workingAge,
                        long cashMinor, long dailyNeedMinor, int satisfaction,
                        long lastSettlementDay, long lastMigrationDay, int health, int education,
                        int unemploymentDays, int employmentDays, int averageAge,
                        int children, int elderly) {
    public Household(String id, String region, int size, int workingAge, long cashMinor,
                     long dailyNeedMinor, int satisfaction, long lastSettlementDay) {
        this(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction,
                lastSettlementDay, -1L, 70, 0, 0, 0, 30,
                Math.max(0, size - workingAge), 0);
    }
    public Household(String id, String region, int size, int workingAge, long cashMinor,
                     long dailyNeedMinor, int satisfaction, long lastSettlementDay,
                     long lastMigrationDay) {
        this(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction,
                lastSettlementDay, lastMigrationDay, 70, 0, 0, 0, 30,
                Math.max(0, size - workingAge), 0);
    }
    public Household(String id, String region, int size, int workingAge, long cashMinor,
                     long dailyNeedMinor, int satisfaction, long lastSettlementDay,
                     long lastMigrationDay, int health, int education) {
        this(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction,
                lastSettlementDay, lastMigrationDay, health, education, 0, 0, 30,
                Math.max(0, size - workingAge), 0);
    }
    public Household(String id, String region, int size, int workingAge, long cashMinor,
                     long dailyNeedMinor, int satisfaction, long lastSettlementDay,
                     long lastMigrationDay, int health, int education, int unemploymentDays) {
        this(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction,
                lastSettlementDay, lastMigrationDay, health, education, unemploymentDays, 0, 30,
                Math.max(0, size - workingAge), 0);
    }
    public Household(String id, String region, int size, int workingAge, long cashMinor,
                     long dailyNeedMinor, int satisfaction, long lastSettlementDay,
                     long lastMigrationDay, int health, int education, int unemploymentDays,
                     int employmentDays) {
        this(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction,
                lastSettlementDay, lastMigrationDay, health, education, unemploymentDays,
                employmentDays, 30, Math.max(0, size - workingAge), 0);
    }
    public Household(String id, String region, int size, int workingAge, long cashMinor,
                     long dailyNeedMinor, int satisfaction, long lastSettlementDay,
                     long lastMigrationDay, int health, int education, int unemploymentDays,
                     int employmentDays, int averageAge) {
        this(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction,
                lastSettlementDay, lastMigrationDay, health, education, unemploymentDays,
                employmentDays, averageAge, Math.max(0, size - workingAge), 0);
    }
    public Household {
        id = id == null ? "" : id.trim();
        region = region == null || region.isBlank() ? "spawn" : region.trim();
        if (id.isBlank() || size <= 0 || workingAge < 0 || workingAge > size
                || cashMinor < 0L || dailyNeedMinor < 0L || satisfaction < 0 || satisfaction > 100
                || lastSettlementDay < -1L || lastMigrationDay < -1L || health < 0 || health > 100
                || education < 0 || education > 100 || unemploymentDays < 0 || unemploymentDays > 10000
                || employmentDays < 0 || employmentDays > 10000 || averageAge < 0 || averageAge > 120
                || children < 0 || elderly < 0 || children + workingAge + elderly != size) {
            throw new IllegalArgumentException("Invalid household");
        }
    }
    public Household withSettlement(long day, long cash, int nextSatisfaction, String nextRegion) {
        return new Household(id, nextRegion, size, workingAge, Math.max(0L, cash), dailyNeedMinor,
                Math.max(0, Math.min(100, nextSatisfaction)), day, lastMigrationDay, health, education,
                unemploymentDays, employmentDays, averageAge, children, elderly);
    }
    public Household withCash(long cash) { return copy(Math.max(0L, cash), region, size, workingAge, averageAge, children, elderly); }
    public Household withRegion(String nextRegion, long day) {
        return new Household(id, nextRegion, size, workingAge, cashMinor, dailyNeedMinor, satisfaction,
                lastSettlementDay, day, health, education, unemploymentDays, employmentDays, averageAge, children, elderly);
    }
    public Household withHumanCapital(int nextHealth, int nextEducation) {
        return copy(cashMinor, region, size, workingAge, averageAge, children, elderly)
                .withRawHumanCapital(nextHealth, nextEducation);
    }
    private Household withRawHumanCapital(int nextHealth, int nextEducation) {
        return new Household(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction,
                lastSettlementDay, lastMigrationDay, nextHealth, nextEducation, unemploymentDays,
                employmentDays, averageAge, children, elderly);
    }
    public Household withEmploymentState(boolean employed) {
        int days = employed ? 0 : Math.min(10000, unemploymentDays + (workingAge > 0 ? 1 : 0));
        int worked = employed ? Math.min(10000, employmentDays + 1) : employmentDays;
        return new Household(id, region, size, workingAge, cashMinor, dailyNeedMinor, satisfaction,
                lastSettlementDay, lastMigrationDay, health, education, days, worked, averageAge, children, elderly);
    }
    public Household withDemographics(int nextSize, int nextWorkingAge, int nextAverageAge) {
        int safeSize = Math.max(1, nextSize);
        int nextChildren = Math.min(children, Math.max(0, safeSize - nextWorkingAge));
        int nextElderly = Math.min(elderly, Math.max(0, safeSize - nextWorkingAge - nextChildren));
        return withCohorts(nextChildren, Math.max(0, safeSize - nextChildren - nextElderly), nextElderly, nextAverageAge);
    }
    public Household withDemographics(int nextSize, int nextWorkingAge) {
        return withDemographics(nextSize, nextWorkingAge, averageAge);
    }
    public Household withCohorts(int nextChildren, int nextWorkingAge, int nextElderly, int nextAverageAge) {
        int safeChildren = Math.max(0, nextChildren), safeWorking = Math.max(0, nextWorkingAge), safeElderly = Math.max(0, nextElderly);
        int safeSize = safeChildren + safeWorking + safeElderly;
        if (safeSize <= 0) return this;
        return new Household(id, region, safeSize, safeWorking, cashMinor, dailyNeedMinor, satisfaction,
                lastSettlementDay, lastMigrationDay, health, education, unemploymentDays, employmentDays,
                Math.max(0, Math.min(120, nextAverageAge)), safeChildren, safeElderly);
    }
    public Household withAnnualAging(long day) {
        if (day <= 0L || day % 365L != 0L) return this;
        int childToAdult = children / 18;
        int adultToElderly = workingAge / 45;
        return withCohorts(children - childToAdult, workingAge + childToAdult - adultToElderly,
                elderly + adultToElderly, Math.min(120, averageAge + 1));
    }
    public Household withBirth() {
        int nextAge = (averageAge * size) / (size + 1);
        return withCohorts(children + 1, workingAge, elderly, nextAge);
    }
    public Household withDeath() {
        if (elderly > 0) return withCohorts(children, workingAge, elderly - 1, averageAge);
        if (children > 0) return withCohorts(children - 1, workingAge, elderly, averageAge);
        if (workingAge > 1) return withCohorts(children, workingAge - 1, elderly, averageAge);
        return this;
    }
    /** Merges another household into this household using population-weighted state. */
    public Household mergeWith(Household other) {
        if (other == null || !region.equals(other.region) || id.equals(other.id)) return null;
        int mergedSize = Math.addExact(size, other.size);
        long mergedCash = add(cashMinor, other.cashMinor);
        long mergedNeed = weightedAverage(dailyNeedMinor, size, other.dailyNeedMinor, other.size, mergedSize);
        int mergedSatisfaction = (int) weightedAverage(satisfaction, size, other.satisfaction, other.size, mergedSize);
        int mergedHealth = (int) weightedAverage(health, size, other.health, other.size, mergedSize);
        int mergedEducation = (int) weightedAverage(education, size, other.education, other.size, mergedSize);
        int mergedAge = (int) weightedAverage(averageAge, size, other.averageAge, other.size, mergedSize);
        int mergedWorkingAge = Math.addExact(workingAge, other.workingAge);
        int mergedChildren = Math.addExact(children, other.children);
        int mergedElderly = Math.addExact(elderly, other.elderly);
        return new Household(id, region, mergedSize, mergedWorkingAge, mergedCash, mergedNeed,
                mergedSatisfaction, Math.max(lastSettlementDay, other.lastSettlementDay),
                Math.max(lastMigrationDay, other.lastMigrationDay), mergedHealth, mergedEducation,
                Math.min(unemploymentDays, other.unemploymentDays), Math.max(employmentDays, other.employmentDays),
                mergedAge, mergedChildren, mergedElderly);
    }
    private Household copy(long cash, String nextRegion, int nextSize, int nextWorkingAge,
                           int nextAverageAge, int nextChildren, int nextElderly) {
        return new Household(id, nextRegion, nextSize, nextWorkingAge, cash, dailyNeedMinor, satisfaction,
                lastSettlementDay, lastMigrationDay, health, education, unemploymentDays, employmentDays,
                nextAverageAge, nextChildren, nextElderly);
    }
    private static long add(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }
    private static long weightedAverage(long left, int leftWeight, long right, int rightWeight, int totalWeight) {
        long leftPart = left > Long.MAX_VALUE / leftWeight ? Long.MAX_VALUE : left * leftWeight;
        long rightPart = right > Long.MAX_VALUE / rightWeight ? Long.MAX_VALUE : right * rightWeight;
        return add(leftPart, rightPart) / totalWeight;
    }
}
