package com.ailudick.capitalismmod.government;

import com.ailudick.capitalismmod.population.Household;
import com.ailudick.capitalismmod.population.PopulationSavedData;
import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.economy.expansion.EconomicEventService;
import com.ailudick.capitalismmod.bond.BondMarket;
import net.minecraft.server.MinecraftServer;

/** Applies one idempotent fiscal-transfer pass per simulated day. */
public final class GovernmentPolicyService {
    private GovernmentPolicyService() {}

    public static int settleDaily(MinecraftServer server, long day) {
        GovernmentPolicySavedData policy = GovernmentPolicySavedData.get(server);
        applyAutomaticInflationPolicy(server, policy, day);
        applyAutomaticOpenMarketPolicy(server, policy, day);
        long benefit = policy.dailyBenefitMinor();
        PopulationSavedData population = PopulationSavedData.get(server);
        stimulateHousing(server, population, policy, day);
        int paid = 0;
        for (Household household : population.households()) {
            var risk = com.ailudick.capitalismmod.population.HouseholdFinancialRiskSavedData.get(server)
                    .latest(household.id());
            boolean highRisk = risk != null && risk.score() >= 70;
            if ((!highRisk && household.unemploymentDays() < 3) || household.unemploymentDays() > 90
                    || (!highRisk && household.employmentDays() < 7) || (!highRisk && household.satisfaction() >= 70)) continue;
            if (population.find(household.id()) == null) continue;
            long maximum = multiply(multiply(household.dailyNeedMinor(), household.size()), 60L) / 100L;
            if (benefit > 0L) {
                long payment = Math.min(benefit, maximum);
                if (payOnce(policy, population, household, day, payment, "government-benefit:")) paid++;
            }
            int supportRate = PublicBudgetEconomics.effectiveRegionalSupportRate(policy.regionalSupportRatePercent(),
                    EconomicEventService.laborDemandShockBps(server, household.region(), server.overworld().getGameTime()));
            if (supportRate > 0 && (regionalUnemployment(server, household.region()) >= 30
                    || regionalShortage(server, household.region()))) {
                long support = multiply(multiply(household.dailyNeedMinor(), household.size()),
                        supportRate) / 100L;
                if (payOnce(policy, population, household, day, support, "government-regional-support:")) paid++;
            }
        }
        return paid;
    }

    private static void applyAutomaticInflationPolicy(MinecraftServer server,
                                                       GovernmentPolicySavedData policy, long day) {
        if (!policy.automaticInflationPolicy()) return;
        InflationSavedData inflation = InflationSavedData.get(server);
        InflationSavedData.Snapshot snapshot = inflation.atOrBefore(day - 1L);
        InflationSavedData.Snapshot previous = inflation.atOrBefore(day - 31L);
        if (snapshot == null || previous == null) return;
        int adjustment = InflationEconomics.policyRateAdjustment(snapshot.indexBps(), previous.indexBps(),
                policy.inflationTargetBps());
        policy.adjustPolicyRateOnce(day, adjustment);
    }

    private static void applyAutomaticOpenMarketPolicy(MinecraftServer server,
                                                        GovernmentPolicySavedData policy, long day) {
        if (!policy.automaticOpenMarketPolicy() || !policy.automaticOpenMarketDue(day)) return;
        InflationSavedData inflation = InflationSavedData.get(server);
        InflationSavedData.Snapshot snapshot = inflation.atOrBefore(day - 1L);
        InflationSavedData.Snapshot previous = inflation.atOrBefore(day - 31L);
        if (snapshot == null || previous == null
                || !InflationEconomics.openMarketPurchaseDue(snapshot.indexBps(), previous.indexBps(),
                policy.inflationTargetBps())) return;
        if (BondMarket.buyBackFirstAvailable(server)) policy.markAutomaticOpenMarketDay(day);
    }

    private static void stimulateHousing(MinecraftServer server, PopulationSavedData population,
                                         GovernmentPolicySavedData policy, long day) {
        var infrastructure = LogisticsInfrastructureSavedData.get(server);
        var construction = PublicConstructionSavedData.get(server);
        java.util.Set<String> regions = new java.util.HashSet<>(infrastructure.regions());
        population.households().forEach(h -> regions.add(h.region()));
        for (String region : regions) {
            int residents = population.population(region);
            var households = population.households().stream().filter(h -> h.region().equals(region)).toList();
            long dailyCosts = PublicBudgetEconomics.dailyMaintenance("housing", infrastructure.count(region, "housing"));
            dailyCosts = add(dailyCosts, PublicBudgetEconomics.dailyMaintenance("school", infrastructure.count(region, "school")));
            dailyCosts = add(dailyCosts, PublicBudgetEconomics.dailyMaintenance("clinic", infrastructure.count(region, "clinic")));
            dailyCosts = add(dailyCosts, multiply(policy.dailyBenefitMinor(), households.size()));
            boolean expandNonEssential = PublicBudgetEconomics.allowNonEssentialExpansion(
                    PublicBudgetEconomics.fiscalStress(policy.treasuryMinor(), dailyCosts));
            startIfNeeded(construction, region, "housing", residents,
                    PublicConstructionEconomics.housingPressure(residents, infrastructure.count(region, "housing")), day);
            startIfNeeded(construction, region, "school", residents, expandNonEssential &&
                    PublicConstructionEconomics.servicePressure(residents, infrastructure.count(region, "school"), 10, 70), day);
            startIfNeeded(construction, region, "clinic", residents, expandNonEssential &&
                    PublicConstructionEconomics.servicePressure(residents, infrastructure.count(region, "clinic"), 10, 70), day);
        }
    }

    private static void startIfNeeded(PublicConstructionSavedData construction, String region, String facility,
                                      int residents, boolean pressure, long day) {
        if (!pressure || residents <= 0 || construction.hasActiveProject(region, facility)) return;
        construction.start("auto-" + facility + ":" + day + ":" + region, region, facility, 1, day);
    }

    private static long multiply(long left, int right) {
        if (left <= 0L || right <= 0) return 0L;
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private static long add(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
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

    /** A cost-of-living shock can justify regional support before mass unemployment appears. */
    private static boolean regionalShortage(MinecraftServer server, String region) {
        CommoditySavedData commodities = CommoditySavedData.get(server);
        for (net.minecraft.world.item.ItemStack stack : Commodities.ALL) {
            String itemId = Commodities.id(stack);
            if (!isEssential(itemId)) continue;
            long global = commodities.price(itemId);
            long regional = commodities.regionalPrice(itemId, region);
            long shock = Math.max(1L, global / 10L);
            long threshold = global > Long.MAX_VALUE - shock ? Long.MAX_VALUE : global + shock;
            if (global > 0L && regional >= threshold) return true;
        }
        return false;
    }

    private static boolean isEssential(String itemId) {
        String id = itemId.toLowerCase(java.util.Locale.ROOT);
        return id.contains("bread") || id.contains("potato") || id.contains("carrot")
                || id.contains("apple") || id.contains("wheat") || id.contains("beef")
                || id.contains("pork");
    }

    private static long multiply(long a, long b) {
        try { return Math.multiplyExact(a, b); }
        catch (ArithmeticException ignored) { return Long.MAX_VALUE; }
    }
}
