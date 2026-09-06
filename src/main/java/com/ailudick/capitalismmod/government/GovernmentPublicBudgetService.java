package com.ailudick.capitalismmod.government;

import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import net.minecraft.server.MinecraftServer;

/** Pays daily upkeep for public facilities and depreciates them when funding is unavailable. */
public final class GovernmentPublicBudgetService {
    private static final String[] FACILITIES = {"housing", "school", "clinic"};

    private GovernmentPublicBudgetService() {}

    public static int settleDaily(MinecraftServer server, long day) {
        GovernmentPolicySavedData policy = GovernmentPolicySavedData.get(server);
        LogisticsInfrastructureSavedData infrastructure = LogisticsInfrastructureSavedData.get(server);
        int maintained = 0;
        for (String region : infrastructure.regions()) {
            for (String facility : FACILITIES) {
                int count = infrastructure.count(region, facility);
                long cost = PublicBudgetEconomics.dailyMaintenance(facility, count);
                if (count <= 0 || cost <= 0L) continue;
                String transactionId = "public-maintenance:" + day + ":" + region + ":" + facility;
                if (policy.hasSpending(transactionId)) {
                    // A repeated settlement observes the existing receipt; it
                    // must not interpret the idempotency hit as a budget failure.
                    maintained++;
                } else if (policy.spend("public-service:" + region, day, cost, transactionId)) {
                    maintained++;
                } else {
                    // One unit fails per day, keeping fiscal stress visible without deleting a city at once.
                    infrastructure.changePublicFacility(region, facility, -1);
                }
            }
        }
        return maintained;
    }
}
