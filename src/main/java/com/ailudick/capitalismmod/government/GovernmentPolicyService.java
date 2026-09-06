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
            if (household.unemploymentDays() < 3 || household.satisfaction() >= 70
                    || policy.treasuryMinor() < benefit) continue;
            String source = "government-benefit:" + day + ":" + household.id();
            if (population.addCashOnce(household.id(), benefit, source)
                    && policy.spend(household.id(), day, benefit, source)) paid++;
        }
        return paid;
    }
}
