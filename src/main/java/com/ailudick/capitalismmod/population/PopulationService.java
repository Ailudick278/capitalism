package com.ailudick.capitalismmod.population;

import com.ailudick.capitalismmod.economy.labor.EmploymentRecord;
import com.ailudick.capitalismmod.economy.labor.LaborMarketSavedData;
import com.ailudick.capitalismmod.economy.labor.LaborMarketService;
import com.ailudick.capitalismmod.economy.expansion.EconomicEventService;
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
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.currency.Money;
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
            // LaborPayrollService is the sole wage settlement authority. It
            // has already credited this household (or its arrears) before the
            // household phase runs; recomputing income here would pay wages twice.
            long cash = household.cashMinor();
            int residents = population.population(household.region());
            int housingUnits = LogisticsInfrastructureSavedData.get(server).count(household.region(), "housing");
            long livingNeed = multiply(household.dailyNeedMinor(), household.size());
            long rentPerResident = CityHousingSavedData.get(server).dailyRent(household.region(), residents, housingUnits);
            long rentDue = multiply(rentPerResident, household.size());
            ConsumptionResult goods = consume(server, household, cash, livingNeed, day);
            HousingLeaseSavedData.Payment rent = HousingLeaseSavedData.get(server).settleRent(household.id(),
                    household.region(), day, rentPerResident, rentDue, goods.remainingCash());
            if (rent.rentPaidMinor() > 0L) {
                String landlord = CityHousingSavedData.get(server).landlord(household.region());
                if ("government".equals(landlord)) {
                    com.ailudick.capitalismmod.government.GovernmentPolicySavedData.get(server)
                            .collectRent(rent.id(), day, household.id(), household.region(), rent.rentPaidMinor());
                } else if (CompanySavedData.get(server).get(landlord) != null) {
                    long rentMajor = Money.toMajorCeiling(rent.rentPaidMinor());
                    CompanyHelper.creditTreasuryNonOperatingOnce(server, landlord, Config.defaultCurrency().id(), rentMajor,
                            "housing_rent", "Household housing rent", rent.id());
                } else if (landlord.startsWith("player:") && validUuid(landlord.substring("player:".length()))) {
                    PrivateLandlordSavedData.get(server).creditOnce(rent.id(), day,
                            landlord.substring("player:".length()), rent.rentPaidMinor());
                } else {
                    // A deleted landlord must not make household funds disappear.
                    com.ailudick.capitalismmod.government.GovernmentPolicySavedData.get(server)
                            .collectRent(rent.id(), day, household.id(), household.region(), rent.rentPaidMinor());
                }
            }
            long remainingCash = Math.max(0L, goods.remainingCash() - rent.paidMinor());
            long totalSpent = add(goods.spent(), rent.paidMinor());
            long totalNeed = add(livingNeed, rentDue);
            int spendingWelfare = totalNeed <= 0L ? 100 : (int) Math.max(0L, Math.min(100L, totalSpent * 100L / totalNeed));
            int serviceWelfare = LogisticsInfrastructureSavedData.get(server)
                    .publicServiceScore(server, household.region(), population.population(household.region()));
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
            Household settled = household.withSettlement(day, remainingCash, welfare, household.region())
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
            int offerLimit = EconomicEventService.effectiveVacancies(offer.vacancies(),
                    EconomicEventService.laborDemandShockBps(server, offer.region(), server.overworld().getGameTime()));
            int offerHired = 0;
            if (offerLimit <= 0) continue;
            for (Household household : population.households()) {
                if (!isNpc(household.id()) || household.workingAge() <= 0 || !labor.activeForWorker(household.id()).isEmpty()) continue;
                boolean local = household.region().equals(offer.region());
                long livingCost = multiply(household.dailyNeedMinor(), household.size());
                long destinationRentPerResident = CityHousingSavedData.get(server).dailyRent(offer.region(),
                        population.population(offer.region()), LogisticsInfrastructureSavedData.get(server)
                                .count(offer.region(), "housing"));
                long destinationRent = multiply(destinationRentPerResident, household.size());
                long commuteCost = HousingEconomics.commuteCost(household.dailyNeedMinor(), household.size(),
                        TradeRegion.distance(household.region(), offer.region()));
                long migrationFriction = local ? 0L : LogisticsInfrastructureSavedData.get(server)
                        .migrationFriction(household.dailyNeedMinor(), household.size(), offer.region());
                int originServices = LogisticsInfrastructureSavedData.get(server)
                        .publicServiceScore(server, household.region(), population.population(household.region()));
                int destinationServices = LogisticsInfrastructureSavedData.get(server)
                        .publicServiceScore(server, offer.region(), population.population(offer.region()));
                long relocationCost = local ? 0L : HousingEconomics.migrationCost(household.dailyNeedMinor(),
                        household.size(), destinationRent / Math.max(1, household.size()), migrationFriction);
                boolean willingToMove = !local && MigrationEconomics.willingToMove(household.satisfaction(),
                        originServices, destinationServices, offer.dailyWageMinor(),
                        add(add(livingCost, destinationRent), add(migrationFriction, commuteCost)));
                if (!local && !willingToMove) continue;
                if (!local && household.cashMinor() < relocationCost) continue;
                LaborMarketService.ensureNpcProfile(server, household.id(), household.workingAge());
                String originalRegion = household.region();
                boolean migrated = local || population.migrate(household.id(), offer.region(), now, relocationCost);
                if (!migrated) continue;
                if (LaborMarketService.hireNpc(server, offer.id(), household.id())) {
                    if (!local) {
                        HousingLeaseSavedData.get(server).relocate(household.id(), offer.region(),
                                destinationRentPerResident);
                    }
                    hired++; offerHired++;
                    if (offerHired >= offerLimit) break;
                    continue;
                }
                if (!local) {
                    population.rollbackMigration(household.id(), originalRegion, offer.region(), now, relocationCost);
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
    private static boolean validUuid(String value) { try { java.util.UUID.fromString(value); return true; } catch (IllegalArgumentException e) { return false; } }
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
            long unitPrice = Math.max(1L, ExchangeRates.convert(Money.toMinorSaturated(priceMajor),
                    Currencies.USD, Config.defaultCurrency()));
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
            if (!warehouse.consumeOnce(InventoryOwner.company(seller.companyId()), item.getItem(), purchasable,
                    source + ":goods")) continue;
            long revenueInUsdMinor = ExchangeRates.convert(actualCost, Config.defaultCurrency(), Currencies.USD);
            long revenueMajor = Money.toMajorCeiling(revenueInUsdMinor);
            if (!alreadyCredited && (revenueMajor <= 0L || !CompanyHelper.creditTreasuryNonOperatingOnce(server, seller.companyId(), "usd", revenueMajor,
                    "household_sales", "Virtual household sale", source))) {
                warehouse.credit(InventoryOwner.company(seller.companyId()), item.getItem(), purchasable);
                continue;
            }
            consumption.record(new HouseholdConsumptionSavedData.Consumption(source, household.id(), day, categories[i][0], itemId, purchasable, unitPrice, actualCost));
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
