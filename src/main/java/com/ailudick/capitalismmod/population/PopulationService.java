package com.ailudick.capitalismmod.population;

import com.ailudick.capitalismmod.economy.labor.EmploymentRecord;
import com.ailudick.capitalismmod.economy.labor.LaborMarketSavedData;
import com.ailudick.capitalismmod.economy.labor.LaborMarketService;
import com.ailudick.capitalismmod.economy.labor.JobOffer;
import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.MinecraftServer;

/** Daily household accounting: wages become household income, needs consume cash, and persistent shortfalls lower welfare. */
public final class PopulationService {
    private PopulationService() {}
    public static void ensurePlayerHousehold(net.minecraft.server.level.ServerPlayer player) {
        if (player == null) return;
        PopulationSavedData data = PopulationSavedData.get(player.getServer());
        String id = player.getUUID().toString();
        if (data.find(id) == null) data.upsert(new Household(id, "spawn", 1, 1, 0L, 1000L, 100, -1L));
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
            ConsumptionResult result = consume(server, household, cash, need, day);
            int welfare = need <= 0L ? 100 : (int) Math.max(0L, Math.min(100L, result.spent() * 100L / need));
            population.upsert(household.withSettlement(day, result.remainingCash(), welfare, household.region()));
        }
    }
    public static int matchResidents(MinecraftServer server, long now) {
        PopulationSavedData population = PopulationSavedData.get(server); LaborMarketSavedData labor = LaborMarketSavedData.get(server); int hired = 0;
        for (JobOffer offer : labor.openOffers(server.overworld().getGameTime())) {
            for (Household household : population.households()) {
                if (!isNpc(household.id()) || household.workingAge() <= 0 || !labor.activeForWorker(household.id()).isEmpty()) continue;
                boolean local = household.region().equals(offer.region());
                boolean willingToMove = !local && household.satisfaction() <= 40
                        && offer.dailyWageMinor() >= household.dailyNeedMinor() * (long) household.size();
                if (!local && !willingToMove) continue;
                LaborMarketService.ensureNpcProfile(server, household.id(), household.workingAge());
                if (LaborMarketService.hireNpc(server, offer.id(), household.id())) {
                    if (!local) population.move(household.id(), offer.region(), now);
                    hired++; break;
                }
            }
        }
        return hired;
    }
    public static int seedNpc(MinecraftServer server, String region, int count) {
        if (server == null || count <= 0 || count > 10000) return 0;
        PopulationSavedData population = PopulationSavedData.get(server); int created = 0;
        for (int i = 0; i < count; i++) { String id = "npc-" + java.util.UUID.randomUUID(); population.upsert(new Household(id, region, 1, 1, 500L, 1000L, 70, -1L)); LaborMarketService.ensureNpcProfile(server, id, 1); created++; }
        return created;
    }
    private static boolean isNpc(String id) { return id != null && id.startsWith("npc-"); }
    private static ConsumptionResult consume(MinecraftServer server, Household household, long cash, long need, long day) {
        if (cash <= 0L || need <= 0L) return new ConsumptionResult(cash, 0L);
        long remaining = cash; long spent = 0L;
        String[][] categories = {{"food", "wheat", "flour", "canned_food", "bread"}, {"energy", "coal", "fuel_oil", "diesel"}, {"living", "planks", "furniture", "wood"}};
        int[] shares = {60, 20, 20};
        for (int i = 0; i < categories.length; i++) {
            ItemStack item = findCommodity(categories[i]);
            if (item == null) continue;
            String itemId = Commodities.id(item); long priceMajor = CommoditySavedData.get(server).price(itemId);
            long unitPrice = Math.max(1L, Math.min(Long.MAX_VALUE / 100L, priceMajor) * 100L);
            long budget = Math.min(remaining, need * shares[i] / 100L);
            long quantity = Math.min((long) household.size() * 4L, budget / unitPrice);
            if (quantity <= 0L) continue;
            long cost = quantity > Long.MAX_VALUE / unitPrice ? Long.MAX_VALUE : quantity * unitPrice;
            String source = "household-consumption:" + household.id() + ":" + day + ":" + categories[i][0];
            HouseholdConsumptionSavedData.get(server).record(new HouseholdConsumptionSavedData.Consumption(source, household.id(), day, categories[i][0], itemId, quantity, unitPrice, cost));
            CommoditySavedData.get(server).addNetVolumeOnce(itemId, -quantity, source);
            remaining -= cost; spent = add(spent, cost);
        }
        return new ConsumptionResult(remaining, spent);
    }
    private static ItemStack findCommodity(String[] names) {
        for (ItemStack stack : Commodities.ALL) { String id = Commodities.id(stack).toLowerCase(java.util.Locale.ROOT); for (int i=1;i<names.length;i++) if (id.contains(names[i])) return stack; }
        return null;
    }
    private record ConsumptionResult(long remainingCash, long spent) {}
    private static long add(long a,long b){try{return Math.addExact(a,b);}catch(ArithmeticException e){return Long.MAX_VALUE;}}
}
