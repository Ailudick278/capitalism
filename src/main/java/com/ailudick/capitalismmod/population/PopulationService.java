package com.ailudick.capitalismmod.population;

import com.ailudick.capitalismmod.economy.labor.EmploymentRecord;
import com.ailudick.capitalismmod.economy.labor.LaborMarketSavedData;
import com.ailudick.capitalismmod.economy.labor.LaborMarketService;
import com.ailudick.capitalismmod.economy.labor.JobOffer;
import com.ailudick.capitalismmod.economy.labor.LaborProfile;
import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import com.ailudick.capitalismmod.market.TradeRegion;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanyLedgerSavedData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.MinecraftServer;

/** Daily household accounting: wages become household income, needs consume cash, and persistent shortfalls lower welfare. */
public final class PopulationService {
    private PopulationService() {}
    public static void ensurePlayerHousehold(net.minecraft.server.level.ServerPlayer player) {
        if (player == null) return;
        PopulationSavedData data = PopulationSavedData.get(player.getServer());
        String id = player.getUUID().toString();
        if (data.find(id) == null) data.upsert(new Household(id, "spawn", 1, 1, 0L, 1000L, 100, -1L, -1L, 80, 20));
    }
    public static void settleDaily(MinecraftServer server, long day) {
        PopulationSavedData population = PopulationSavedData.get(server);
        LaborMarketSavedData labor = LaborMarketSavedData.get(server);
        for (Household household : population.households()) {
            if (household.lastSettlementDay() >= day) continue;
            long income = isNpc(household.id()) ? 0L : labor.employments().stream().filter(e -> e.active() && e.workerId().equals(household.id()))
                    .mapToLong(EmploymentRecord::dailyWageMinor).reduce(0L, PopulationService::add);
            long cash = add(household.cashMinor(), income);
            int residents = population.population(household.region());
            int housingUnits = LogisticsInfrastructureSavedData.get(server).count(household.region(), "housing");
            long need = add(multiply(household.dailyNeedMinor(), household.size()),
                    multiply(CityHousingSavedData.get(server).dailyRent(household.region(), residents, housingUnits), household.size()));
            ConsumptionResult result = consume(server, household, cash, need, day);
            int spendingWelfare = need <= 0L ? 100 : (int) Math.max(0L, Math.min(100L, result.spent() * 100L / need));
            int serviceWelfare = LogisticsInfrastructureSavedData.get(server)
                    .publicServiceScore(household.region(), population.population(household.region()));
            int welfare = spendingWelfare * 70 / 100 + serviceWelfare * 30 / 100;
            int clinicCoverage = population.population(household.region()) <= 0 ? 100
                    : Math.min(100, LogisticsInfrastructureSavedData.get(server).count(household.region(), "clinic")
                    * 1000 / population.population(household.region()));
            int schoolCoverage = population.population(household.region()) <= 0 ? 100
                    : Math.min(100, LogisticsInfrastructureSavedData.get(server).count(household.region(), "school")
                    * 1000 / population.population(household.region()));
            int health = EducationHealthEconomics.nextHealth(household.health(), clinicCoverage, spendingWelfare);
            int education = EducationHealthEconomics.nextEducation(household.education(), schoolCoverage, household.workingAge());
            boolean employed = !labor.activeForWorker(household.id()).isEmpty();
            Household settled = household.withSettlement(day, result.remainingCash(), welfare, household.region())
                    .withEmploymentState(employed)
                    .withHumanCapital(health, education)
                    .withAnnualAging(day);
            population.upsert(settled);
            LaborProfile profile = labor.profile(household.id());
            if (profile != null) labor.registerProfile(profile.withHealthAndEducation(health, education));
            evolveNpc(population, settled, day);
        }
    }
    public static int matchResidents(MinecraftServer server, long now) {
        PopulationSavedData population = PopulationSavedData.get(server); LaborMarketSavedData labor = LaborMarketSavedData.get(server); int hired = 0;
        for (JobOffer offer : labor.openOffers(server.overworld().getGameTime())) {
            for (Household household : population.households()) {
                if (!isNpc(household.id()) || household.workingAge() <= 0 || !labor.activeForWorker(household.id()).isEmpty()) continue;
                boolean local = household.region().equals(offer.region());
                long livingCost = multiply(household.dailyNeedMinor(), household.size());
                long destinationRent = multiply(CityHousingSavedData.get(server).dailyRent(offer.region(),
                        population.population(offer.region()), LogisticsInfrastructureSavedData.get(server)
                                .count(offer.region(), "housing")), household.size());
                long commuteCost = HousingEconomics.commuteCost(household.dailyNeedMinor(), household.size(),
                        TradeRegion.distance(household.region(), offer.region()));
                long migrationFriction = local ? 0L : LogisticsInfrastructureSavedData.get(server)
                        .migrationFriction(household.dailyNeedMinor(), household.size(), offer.region());
                boolean willingToMove = !local && household.satisfaction() <= 40
                        && offer.dailyWageMinor() >= add(add(livingCost, destinationRent), add(migrationFriction, commuteCost));
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
        for (int i = 0; i < count; i++) { String id = "npc-" + java.util.UUID.randomUUID(); population.upsert(new Household(id, region, 1, 1, 500L, 1000L, 70, -1L, -1L, 70, 10)); LaborMarketService.ensureNpcProfile(server, id, 1); created++; }
        return created;
    }
    private static boolean isNpc(String id) { return id != null && id.startsWith("npc-"); }
    private static void evolveNpc(PopulationSavedData population, Household household, long day) {
        if (!isNpc(household.id()) || day < 0L) return;
        if (DemographicEconomics.eventFor(household.id() + ":birth", day,
                DemographicEconomics.annualBirthRatePerThousand(household.satisfaction(), household.averageAge()), household.size())) {
            population.upsert(household.withBirth());
            return;
        }
        if (household.size() > 1 && DemographicEconomics.eventFor(household.id() + ":death", day,
                DemographicEconomics.annualDeathRatePerThousand(household.health(), household.averageAge()), household.size())) {
            population.upsert(household.withDeath());
        }
    }
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
            String source = "household-consumption:" + household.id() + ":" + day + ":" + categories[i][0];
            long cost = quantity > Long.MAX_VALUE / unitPrice ? Long.MAX_VALUE : quantity * unitPrice;
            HouseholdConsumptionSavedData consumption = HouseholdConsumptionSavedData.get(server);
            HouseholdConsumptionSavedData.Consumption previous = consumption.records().stream().filter(r -> r.id().equals(source)).findFirst().orElse(null);
            if (previous != null) { remaining -= previous.totalCostMinor(); spent = add(spent, previous.totalCostMinor()); continue; }
            Company seller = findSeller(server, itemId, (int) Math.min(Integer.MAX_VALUE, quantity));
            if (seller == null) continue;
            WarehouseSavedData warehouse = WarehouseSavedData.get(server);
            int purchasable = (int) Math.min((long) Integer.MAX_VALUE, Math.min(quantity, warehouse.count(InventoryOwner.company(seller.companyId()), itemId)));
            if (purchasable <= 0) continue;
            long actualCost = purchasable > Long.MAX_VALUE / unitPrice
                    ? Long.MAX_VALUE : purchasable * unitPrice;
            boolean alreadyCredited = hasCompanySource(server, seller.companyId(), source);
            if (!alreadyCredited && !warehouse.consume(InventoryOwner.company(seller.companyId()), item.getItem(), purchasable)) continue;
            long revenueMajor = actualCost / 100L;
            if (!alreadyCredited && (revenueMajor <= 0L || !CompanyHelper.creditTreasuryNonOperatingOnce(server, seller.companyId(), "usd", revenueMajor,
                    "household_sales", "Virtual household sale", source))) {
                warehouse.credit(InventoryOwner.company(seller.companyId()), item.getItem(), purchasable);
                continue;
            }
            consumption.record(new HouseholdConsumptionSavedData.Consumption(source, household.id(), day, categories[i][0], itemId, purchasable, unitPrice, actualCost));
            CommoditySavedData.get(server).addNetVolumeOnce(itemId, -purchasable, source);
            remaining -= actualCost; spent = add(spent, actualCost);
        }
        return new ConsumptionResult(remaining, spent);
    }
    private static ItemStack findCommodity(String[] names) {
        for (ItemStack stack : Commodities.ALL) { String id = Commodities.id(stack).toLowerCase(java.util.Locale.ROOT); for (int i=1;i<names.length;i++) if (id.contains(names[i])) return stack; }
        return null;
    }
    private static Company findSeller(MinecraftServer server, String itemId, int quantity) {
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        for (Company company : CompanySavedData.get(server).companies().values()) {
            if (company == null || company.treasuryOf("usd") < 0L) continue;
            if (warehouse.count(InventoryOwner.company(company.companyId()), itemId) >= quantity) return company;
        }
        return null;
    }
    private static boolean hasCompanySource(MinecraftServer server, String companyId, String source) {
        return CompanyLedgerSavedData.get(server).entries(companyId).stream()
                .anyMatch(entry -> entry.description() != null && entry.description().contains("[source=" + source + "]"));
    }
    private record ConsumptionResult(long remainingCash, long spent) {}
    private static long multiply(long a, long b) { try { return Math.multiplyExact(a, b); } catch (ArithmeticException e) { return Long.MAX_VALUE; } }
    private static long add(long a,long b){try{return Math.addExact(a,b);}catch(ArithmeticException e){return Long.MAX_VALUE;}}
}
