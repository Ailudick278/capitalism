package com.ailudick.capitalismmod.population;

import com.ailudick.capitalismmod.economy.labor.EmploymentRecord;
import com.ailudick.capitalismmod.economy.labor.LaborMarketSavedData;
import com.ailudick.capitalismmod.economy.labor.LaborMarketService;
import com.ailudick.capitalismmod.economy.labor.JobOffer;
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
            long income = isNpc(household.id()) ? 0L : labor.employments().stream().filter(e -> e.active() && e.workerId().equals(household.id()))
                    .mapToLong(EmploymentRecord::dailyWageMinor).reduce(0L, PopulationService::add);
            long cash = add(household.cashMinor(), income);
            long need = add(0L, household.dailyNeedMinor() * (long) household.size());
            long consumed = Math.min(cash, need);
            int welfare = need <= 0L ? 100 : (int) Math.max(0L, Math.min(100L, consumed * 100L / need));
            population.upsert(household.withSettlement(day, cash - consumed, welfare, household.region()));
        }
    }
    public static int matchResidents(MinecraftServer server, long now) {
        PopulationSavedData population = PopulationSavedData.get(server); LaborMarketSavedData labor = LaborMarketSavedData.get(server); int hired = 0;
        for (JobOffer offer : labor.openOffers(server.overworld().getGameTime())) {
            for (Household household : population.households()) {
                if (!isNpc(household.id()) || household.workingAge() <= 0 || !labor.activeForWorker(household.id()).isEmpty()) continue;
                LaborMarketService.ensureNpcProfile(server, household.id(), household.workingAge());
                if (LaborMarketService.hireNpc(server, offer.id(), household.id())) { hired++; break; }
            }
        }
        return hired;
    }
    public static int seedNpc(MinecraftServer server, String region, int count) {
        if (server == null || count <= 0 || count > 10000) return 0;
        PopulationSavedData population = PopulationSavedData.get(server); int created = 0;
        for (int i = 0; i < count; i++) { String id = "npc-" + java.util.UUID.randomUUID(); population.upsert(new Household(id, region, 1, 1, 500L, 100L, 70, -1L)); LaborMarketService.ensureNpcProfile(server, id, 1); created++; }
        return created;
    }
    private static boolean isNpc(String id) { return id != null && id.startsWith("npc-"); }
    private static long add(long a,long b){try{return Math.addExact(a,b);}catch(ArithmeticException e){return Long.MAX_VALUE;}}
}
