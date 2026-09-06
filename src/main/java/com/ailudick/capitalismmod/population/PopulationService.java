package com.ailudick.capitalismmod.population;

import com.ailudick.capitalismmod.economy.labor.EmploymentRecord;
import com.ailudick.capitalismmod.economy.labor.LaborMarketSavedData;
import net.minecraft.server.MinecraftServer;

/** Daily household accounting: wages become household income, needs consume cash, and persistent shortfalls lower welfare. */
public final class PopulationService {
    private PopulationService() {}
    public static void ensurePlayerHousehold(net.minecraft.server.level.ServerPlayer player) {
        if (player == null) return;
        PopulationSavedData data = PopulationSavedData.get(player.getServer());
        String id = player.getUUID().toString();
        if (data.find(id) == null) data.upsert(new Household(id, "spawn", 1, 1, 0L, 100L, 100, -1L));
    }
    public static void settleDaily(MinecraftServer server, long day) {
        PopulationSavedData population = PopulationSavedData.get(server);
        LaborMarketSavedData labor = LaborMarketSavedData.get(server);
        for (Household household : population.households()) {
            if (household.lastSettlementDay() >= day) continue;
            long income = labor.employments().stream().filter(e -> e.active() && e.workerId().equals(household.id()))
                    .mapToLong(EmploymentRecord::dailyWageMinor).reduce(0L, PopulationService::add);
            long cash = add(household.cashMinor(), income);
            long need = add(0L, household.dailyNeedMinor() * (long) household.size());
            long consumed = Math.min(cash, need);
            int welfare = need <= 0L ? 100 : (int) Math.max(0L, Math.min(100L, consumed * 100L / need));
            population.upsert(household.withSettlement(day, cash - consumed, welfare, household.region()));
        }
    }
    private static long add(long a,long b){try{return Math.addExact(a,b);}catch(ArithmeticException e){return Long.MAX_VALUE;}}
}
