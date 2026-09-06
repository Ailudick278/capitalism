package com.ailudick.capitalismmod.government;

import com.ailudick.capitalismmod.population.Household;
import com.ailudick.capitalismmod.population.PopulationSavedData;
import net.minecraft.server.MinecraftServer;

/** Applies one idempotent fiscal-transfer pass per simulated day. */
public final class GovernmentPolicyService {
    private GovernmentPolicyService() {}

    public static int settleDaily(MinecraftServer server, long day) {
        GovernmentPolicySavedData policy = GovernmentPolicySavedData.get(server);
        long benefit = policy.dailyBenefitMinor();
        if (benefit <= 0L) return 0;
        PopulationSavedData population = PopulationSavedData.get(server);
        int paid = 0;
        for (Household household : population.households()) {
            if (household.unemploymentDays() < 3 || household.unemploymentDays() > 90
                    || household.employmentDays() < 7 || household.satisfaction() >= 70) continue;
            long maximum = multiply(multiply(household.dailyNeedMinor(), household.size()), 60L) / 100L;
            long payment = Math.min(benefit, maximum);
            if (payment <= 0L) continue;
            if (population.find(household.id()) == null) continue;
            String source = "government-benefit:" + day + ":" + household.id();
            // Persist the government-side receipt first. If the server stops
            // before the household update, the next pass sees this receipt and
            // completes the household credit without spending twice.
            boolean alreadySpent = policy.hasSpending(source);
            if (!alreadySpent && (policy.treasuryMinor() < payment
                    || !policy.spend(household.id(), day, payment, source))) continue;
            if (population.addCashOnce(household.id(), payment, source)) paid++;
        }
        return paid;
    }

    private static long multiply(long a, long b) {
        try { return Math.multiplyExact(a, b); }
        catch (ArithmeticException ignored) { return Long.MAX_VALUE; }
    }
}
