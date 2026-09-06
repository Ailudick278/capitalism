package com.ailudick.capitalismmod.government;

import com.ailudick.capitalismmod.population.Household;
import com.ailudick.capitalismmod.population.PopulationSavedData;
import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import net.minecraft.server.MinecraftServer;

/** Applies one idempotent fiscal-transfer pass per simulated day. */
public final class GovernmentPolicyService {
    private GovernmentPolicyService() {}

    public static int settleDaily(MinecraftServer server, long day) {
        GovernmentPolicySavedData policy = GovernmentPolicySavedData.get(server);
        long benefit = policy.dailyBenefitMinor();
        PopulationSavedData population = PopulationSavedData.get(server);
        stimulateHousing(server, population, day);
        int paid = 0;
        for (Household household : population.households()) {
            if (household.unemploymentDays() < 3 || household.unemploymentDays() > 90
                    || household.employmentDays() < 7 || household.satisfaction() >= 70) continue;
            if (population.find(household.id()) == null) continue;
            long maximum = multiply(multiply(household.dailyNeedMinor(), household.size()), 60L) / 100L;
            if (benefit > 0L) {
                long payment = Math.min(benefit, maximum);
                if (payOnce(policy, population, household, day, payment, "government-benefit:")) paid++;
            }
            if (policy.regionalSupportRatePercent() > 0 && regionalUnemployment(server, household.region()) >= 30) {
                long support = multiply(multiply(household.dailyNeedMinor(), household.size()),
                        policy.regionalSupportRatePercent()) / 100L;
                if (payOnce(policy, population, household, day, support, "government-regional-support:")) paid++;
            }
        }
        return paid;
    }

    private static void stimulateHousing(MinecraftServer server, PopulationSavedData population, long day) {
        var infrastructure = LogisticsInfrastructureSavedData.get(server);
        var construction = PublicConstructionSavedData.get(server);
        java.util.Set<String> regions = new java.util.HashSet<>(infrastructure.regions());
        population.households().forEach(h -> regions.add(h.region()));
        for (String region : regions) {
            if (PublicConstructionEconomics.housingPressure(population.population(region),
                    infrastructure.count(region, "housing")) && !construction.hasActiveProject(region, "housing")) {
                construction.start("auto-housing:" + day + ":" + region, region, "housing", 1, day);
            }
        }
    }

    private static boolean payOnce(GovernmentPolicySavedData policy, PopulationSavedData population,
                                   Household household, long day, long payment, String prefix) {
        if (payment <= 0L) return false;
        String source = prefix + day + ":" + household.id();
        boolean alreadySpent = policy.hasSpending(source);
        if (!alreadySpent && (policy.treasuryMinor() < payment
                || !policy.spend(household.id(), day, payment, source))) return false;
        // Persist the government-side receipt first; the household-side source
        // makes replay safe if the server stops between the two writes.
        return population.addCashOnce(household.id(), payment, source);
    }

    private static int regionalUnemployment(MinecraftServer server, String region) {
        var history = CityStatisticsSavedData.get(server).snapshots(region, 1);
        return history.isEmpty() ? 0 : history.get(history.size() - 1).unemploymentRate();
    }

    private static long multiply(long a, long b) {
        try { return Math.multiplyExact(a, b); }
        catch (ArithmeticException ignored) { return Long.MAX_VALUE; }
    }
}
