package com.ailudick.capitalismmod.population;

import com.ailudick.capitalismmod.economy.labor.EmploymentRecord;
import com.ailudick.capitalismmod.economy.labor.LaborMarketSavedData;
import com.ailudick.capitalismmod.economy.labor.LaborMarketService;
import com.ailudick.capitalismmod.economy.labor.LaborPayrollSavedData;
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
import com.ailudick.capitalismmod.government.GovernmentPolicySavedData;
import com.ailudick.capitalismmod.bank.BankAccountHelper;
import com.ailudick.capitalismmod.bank.BankTransaction;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
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
        NpcBankingService.writeOffBadDebts(server, day);
        NpcBankingService.applyDailyInterest(server, day);
        for (Household household : population.households()) {
            if (household.lastSettlementDay() >= day) continue;
            // LaborPayrollService is the sole wage settlement authority. It
            // has already credited this household (or its arrears) before the
            // household phase runs; recomputing income here would pay wages twice.
            long openingCash = household.cashMinor();
            NpcBankingService.Result banking = NpcBankingService.prepare(server, household, day);
            household = banking.household();
            long bankNetCash = banking.bankNetCashMinor();
            long wageIncome = wageIncome(server, household.id(), day);
            long governmentIncome = governmentIncome(server, household.id(), day);
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
            NpcBankingService.Result bankFinish = NpcBankingService.finish(server, settled, day);
            settled = bankFinish.household();
            bankNetCash = add(bankNetCash, bankFinish.bankNetCashMinor());
            population.upsert(settled);
            HouseholdCashflowSavedData.get(server).record(new HouseholdCashflowSavedData.Snapshot(
                    "household-cashflow:" + household.id() + ":" + day, household.id(), day,
                    openingCash, wageIncome, governmentIncome, goods.spent(), rent.rentPaidMinor(), bankNetCash,
                    settled.cashMinor()));
            recordTaxPeriod(server, household.id(), day);
            recordFinancialRisk(server, settled, day);
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
                HouseholdFinancialRiskSavedData.Assessment risk = HouseholdFinancialRiskSavedData.get(server)
                        .latest(household.id());
                boolean willingToMove = !local && MigrationEconomics.willingToMove(household.satisfaction(),
                        originServices, destinationServices, offer.dailyWageMinor(),
                        add(add(livingCost, destinationRent), add(migrationFriction, commuteCost)),
                        risk == null ? 0 : risk.score());
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
            long netUnitPrice = Math.max(1L, ExchangeRates.convert(Money.toMinorSaturated(priceMajor),
                    Currencies.USD, Config.defaultCurrency()));
            long taxPerUnit = vatFor(netUnitPrice);
            long unitPrice = add(netUnitPrice, taxPerUnit);
            long budget = Math.min(remaining, need * shares[i] / 100L);
            long quantity = Math.min((long) household.size() * 4L, budget / unitPrice);
            if (quantity <= 0L) continue;
            String source = "household-consumption:" + household.id() + ":" + day + ":" + categories[i][0];
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
            long taxAmount = taxPerUnit <= 0L ? 0L : (purchasable > Long.MAX_VALUE / taxPerUnit
                    ? Long.MAX_VALUE : purchasable * taxPerUnit);
            long netCost = actualCost > taxAmount ? actualCost - taxAmount : 0L;
            boolean alreadyCredited = hasCompanySource(server, seller.companyId(), source);
            if (!warehouse.consumeOnce(InventoryOwner.company(seller.companyId()), item.getItem(), purchasable,
                    source + ":goods")) continue;
            long revenueInUsdMinor = ExchangeRates.convert(netCost, Config.defaultCurrency(), Currencies.USD);
            long revenueMajor = Money.toMajorCeiling(revenueInUsdMinor);
            if (!alreadyCredited && (revenueMajor <= 0L || !CompanyHelper.creditTreasuryNonOperatingOnce(server, seller.companyId(), "usd", revenueMajor,
                    "household_sales", "Virtual household sale", source))) {
                warehouse.credit(InventoryOwner.company(seller.companyId()), item.getItem(), purchasable);
                continue;
            }
            if (taxAmount > 0L && !GovernmentPolicySavedData.get(server).collectConsumptionTax(
                    source, day, household.id(), seller.companyId(), actualCost, taxAmount)) {
                warehouse.credit(InventoryOwner.company(seller.companyId()), item.getItem(), purchasable);
                continue;
            }
            consumption.record(new HouseholdConsumptionSavedData.Consumption(source, household.id(), day,
                    categories[i][0], itemId, purchasable, unitPrice, actualCost, taxAmount));
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
    private static long wageIncome(MinecraftServer server, String householdId, long day) {
        long total = 0L;
        for (LaborPayrollSavedData.Payment payment : LaborPayrollSavedData.get(server).payments().values()) {
            if (day == payment.day() && householdId.equals(payment.workerId())) {
                total = add(total, ExchangeRates.convert(payment.netMinor(), Currencies.USD, Config.defaultCurrency()));
            }
        }
        return total;
    }
    private static long governmentIncome(MinecraftServer server, String householdId, long day) {
        long total = 0L;
        for (GovernmentPolicySavedData.Transaction transaction : GovernmentPolicySavedData.get(server).transactions()) {
            if (day == transaction.day() && householdId.equals(transaction.householdId())) {
                total = add(total, transaction.amount());
            }
        }
        return total;
    }
    private static void recordTaxPeriod(MinecraftServer server, String householdId, long day) {
        if (day < 29L || day % 30L != 29L) return;
        long start = day - 29L;
        long grossWages = 0L, wageTax = 0L, consumption = 0L, consumptionTax = 0L;
        for (LaborPayrollSavedData.Payment payment : LaborPayrollSavedData.get(server).payments().values()) {
            if (householdId.equals(payment.workerId()) && payment.day() >= start && payment.day() <= day) {
                grossWages = add(grossWages, ExchangeRates.convert(payment.amountMinor(), Currencies.USD, Config.defaultCurrency()));
                wageTax = add(wageTax, ExchangeRates.convert(payment.taxMinor(), Currencies.USD, Config.defaultCurrency()));
            }
        }
        for (HouseholdConsumptionSavedData.Consumption purchase : HouseholdConsumptionSavedData.get(server).records()) {
            if (householdId.equals(purchase.householdId()) && purchase.day() >= start && purchase.day() <= day) {
                consumption = add(consumption, purchase.totalCostMinor());
                consumptionTax = add(consumptionTax, purchase.taxMinor());
            }
        }
        HouseholdTaxPeriodSavedData.get(server).record(new HouseholdTaxPeriodSavedData.Assessment(
                "household-tax-period:" + householdId + ":" + start + ":" + day,
                householdId, start, day, grossWages, wageTax, consumption, consumptionTax));
    }
    private static void recordFinancialRisk(MinecraftServer server, Household household, long day) {
        long rentArrears = 0L;
        HousingLeaseSavedData.Lease lease = HousingLeaseSavedData.get(server).lease(household.id());
        if (lease != null) rentArrears = lease.arrearsMinor();
        long wageArrears = 0L;
        LaborMarketSavedData labor = LaborMarketSavedData.get(server);
        for (EmploymentRecord employment : labor.activeForWorker(household.id())) {
            long usd = LaborPayrollSavedData.get(server).account(employment.id()).unpaid();
            wageArrears = add(wageArrears, ExchangeRates.convert(usd, Currencies.USD, Config.defaultCurrency()));
        }
        long bankDebt = 0L;
        boolean bankOverdue = false;
        int recentRepayments = 0;
        long recentRepaymentMinor = 0L;
        try {
            net.minecraft.server.level.ServerPlayer player = server.getPlayerList()
                    .getPlayer(java.util.UUID.fromString(household.id()));
            if (player != null) {
                for (var account : BankAccountHelper.getAccounts(player).values()) {
                    for (var debt : account.debts().entrySet()) {
                        if (debt.getValue() > 0L && Currencies.exists(debt.getKey())) {
                            bankDebt = add(bankDebt, ExchangeRates.convert(debt.getValue(),
                                    Currencies.byId(debt.getKey()), Config.defaultCurrency()));
                        }
                    }
                    bankOverdue |= account.loanDaysRemaining() < 0
                            && account.debts().values().stream().anyMatch(value -> value > 0L);
                    long repaymentSince = server.overworld().getGameTime() - PerpetualCalendar.ticksForDays(90L);
                    for (BankTransaction transaction : account.transactions()) {
                        if ("repay".equals(transaction.type()) && transaction.amount() < 0L
                                && transaction.reference().startsWith("bank-repayment:")
                                && Currencies.exists(transaction.currencyId())
                                && transaction.occurredAt() >= repaymentSince) {
                            recentRepayments++;
                            recentRepaymentMinor = add(recentRepaymentMinor,
                                    ExchangeRates.convert(Math.abs(transaction.amount()),
                                            Currencies.byId(transaction.currencyId()), Config.defaultCurrency()));
                        }
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
            // NPC households do not yet have bank accounts.
        }
        if (household.id().startsWith("npc-")) {
            NpcBankingSavedData.Account account = NpcBankingSavedData.get(server).find(household.id());
            if (account != null) {
                bankDebt = add(bankDebt, account.debtMinor());
                bankOverdue = account.loanDaysRemaining() < 0 && account.debtMinor() > 0L;
                long since = Math.max(0L, day - 89L);
                for (NpcBankingSavedData.Transaction transaction : account.transactions()) {
                    if ("repayment".equals(transaction.type()) && transaction.day() >= since && transaction.day() <= day) {
                        recentRepayments++;
                        recentRepaymentMinor = add(recentRepaymentMinor, Math.max(0L, -transaction.amountMinor()));
                    }
                }
            }
        }
        long recentIncomeMinor = 0L;
        for (LaborPayrollSavedData.Payment payment : LaborPayrollSavedData.get(server).payments().values()) {
            if (household.id().equals(payment.workerId()) && payment.day() >= Math.max(0L, day - 89L)
                    && payment.day() <= day) {
                recentIncomeMinor = add(recentIncomeMinor,
                        ExchangeRates.convert(payment.amountMinor(), Currencies.USD, Config.defaultCurrency()));
            }
        }
        long debtServiceRatioBps = ratioBps(recentRepaymentMinor, recentIncomeMinor);
        long householdNeed = multiply(household.dailyNeedMinor(), household.size());
        int score = HouseholdFinancialRisk.score(household.cashMinor(), householdNeed,
                rentArrears, wageArrears, bankDebt, household.unemploymentDays(), bankOverdue,
                recentRepayments, debtServiceRatioBps);
        HouseholdFinancialRiskSavedData.get(server).record(new HouseholdFinancialRiskSavedData.Assessment(
                "household-risk:" + household.id() + ":" + day, household.id(), day, household.cashMinor(),
                householdNeed, rentArrears, wageArrears, bankDebt,
                household.unemploymentDays(), recentRepayments, recentRepaymentMinor, recentIncomeMinor,
                debtServiceRatioBps, score, bankOverdue));
    }
    private static long vatFor(long netUnitPrice) {
        if (netUnitPrice <= 0L || Config.VAT_RATE.get() <= 0.0) return 0L;
        double calculated = Math.ceil(netUnitPrice * Config.VAT_RATE.get());
        return !Double.isFinite(calculated) || calculated >= Long.MAX_VALUE
                ? Long.MAX_VALUE : Math.max(0L, (long) calculated);
    }
    private record ConsumptionResult(long remainingCash, long spent) {}
    private static long multiply(long a, long b) { try { return Math.multiplyExact(a, b); } catch (ArithmeticException e) { return Long.MAX_VALUE; } }
    private static long add(long a,long b){try{return Math.addExact(a,b);}catch(ArithmeticException e){return Long.MAX_VALUE;}}
    private static long ratioBps(long numerator, long denominator) {
        if (numerator <= 0L) return 0L;
        if (denominator <= 0L) return 10_000L;
        return numerator > Long.MAX_VALUE / 10_000L
                ? 10_000L : Math.min(10_000L, numerator * 10_000L / denominator);
    }
}
