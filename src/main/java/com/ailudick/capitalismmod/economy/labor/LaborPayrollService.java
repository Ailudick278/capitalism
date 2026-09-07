package com.ailudick.capitalismmod.economy.labor;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.company.CompanyLedgerEntry;
import com.ailudick.capitalismmod.company.CompanyLedgerSavedData;
import com.ailudick.capitalismmod.company.CompanyPayrollService;
import com.ailudick.capitalismmod.company.CompanyLifecycleService;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.ailudick.capitalismmod.population.PopulationSavedData;
import com.ailudick.capitalismmod.government.GovernmentPolicySavedData;
import com.ailudick.capitalismmod.economy.contract.EconomicContractBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Settles individual employment wages and carries arrears until funds exist. */
public final class LaborPayrollService {
    private LaborPayrollService() {}
    public static void settleDaily(MinecraftServer server, long day) {
        LaborMarketSavedData labor = LaborMarketSavedData.get(server);
        LaborPayrollSavedData payroll = LaborPayrollSavedData.get(server);
        for (EmploymentRecord employment : labor.employments()) {
            if (!employment.active() || payroll.account(employment.id()).lastSettlementDay() >= day) continue;
            Company company = CompanySavedData.get(server).get(employment.employerId());
            if (company == null) {
                if (labor.endEmployment(employment.id(), server.overworld().getGameTime())) {
                    EconomicContractBridge.employmentEnded(server, employment.id(), server.overworld().getGameTime());
                }
                continue;
            }
            if (!CompanyLifecycleService.canOperate(server, company.companyId())) {
                if (labor.endEmployment(employment.id(), server.overworld().getGameTime())) {
                    EconomicContractBridge.employmentEnded(server, employment.id(), server.overworld().getGameTime());
                }
                continue;
            }
            long due;
            try { due = Math.addExact(payroll.account(employment.id()).unpaid(), employment.dailyWageMinor()); }
            catch (ArithmeticException e) { due = Long.MAX_VALUE; }
            long contribution = CompanyPayrollService.employerContribution(employment.dailyWageMinor());
            long laborCost;
            try { laborCost = Math.addExact(employment.dailyWageMinor(), contribution); }
            catch (ArithmeticException e) { laborCost = Long.MAX_VALUE; }
            CompanyHelper.recordNonCashExpense(server, company, "labor_payroll:" + employment.id() + ":" + day,
                    Money.toMajorCeiling(laborCost), Currencies.USD.id(), "Employment gross wages and employer contribution");
            String source = "employment-payroll:" + employment.id() + ":" + day;
            CompanyLedgerEntry previousDebit = CompanyLedgerSavedData.get(server).entries(company.companyId()).stream()
                    .filter(entry -> entry.amount() < 0L && entry.description() != null && entry.description().contains("[source=" + source + "]"))
                    .findFirst().orElse(null);
            long availableMinor = company.treasuryOf(Currencies.USD.id()) >= Long.MAX_VALUE / Money.MINOR_UNITS_PER_UNIT
                    ? Long.MAX_VALUE : Math.max(0L, company.treasuryOf(Currencies.USD.id())) * Money.MINOR_UNITS_PER_UNIT;
            long paid = previousDebit == null ? Math.min(availableMinor, due) : toMinor(previousDebit.amount());
            long paidMajor = paid / Money.MINOR_UNITS_PER_UNIT;
            paid = paidMajor * Money.MINOR_UNITS_PER_UNIT;
            boolean delivered = false;
            String householdSource = source;
            if (paidMajor > 0L && (previousDebit != null || CompanyHelper.debitTreasuryNonOperatingOnce(server, company.companyId(), Currencies.USD.id(), paidMajor,
                    "employment_payroll", "Employment wage payment", source))) {
                long tax = withholdingTax(paid);
                long net = paid - tax;
                boolean taxCollected = tax <= 0L || GovernmentPolicySavedData.get(server)
                        .collectWageTax(source + ":tax", day, employment.workerId(), employment.id(), paid, tax);
                if (!taxCollected) {
                    paid = 0L;
                    payroll.settle(employment.id(), day, due);
                    continue;
                }
                try {
                    java.util.UUID workerId = java.util.UUID.fromString(employment.workerId());
                    MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
                    boolean mailboxDelivered = mailbox.hasCreditSource(source)
                            || mailbox.creditMoneyOnce(workerId, Currencies.USD.id(), net, source);
                    ServerPlayer worker = server.getPlayerList().getPlayer(workerId);
                    if (worker != null && mailboxDelivered) mailbox.redeemMoneyOnly(worker);
                    householdSource = source + ":household";
                    PopulationSavedData population = PopulationSavedData.get(server);
                    boolean householdDelivered = population.hasCreditedSource(householdSource)
                            || population.addCashOnce(workerId.toString(), ExchangeRates.convert(net, Currencies.USD, Config.defaultCurrency()), householdSource);
                    delivered = mailboxDelivered && householdDelivered;
                } catch (IllegalArgumentException npcWorker) {
                    delivered = PopulationSavedData.get(server).hasCreditedSource(source)
                            || PopulationSavedData.get(server).addCashOnce(employment.workerId(),
                            ExchangeRates.convert(net, Currencies.USD, Config.defaultCurrency()), source);
                }
            }
            if (delivered) {
                payroll.recordPayment(new LaborPayrollSavedData.Payment(source, employment.id(), employment.workerId(), day,
                        paid, withholdingTax(paid), paid - withholdingTax(paid), householdSource));
                payroll.settle(employment.id(), day, due - paid);
            } else {
                paid = 0L;
                payroll.settle(employment.id(), day, due);
            }
            long arrears = payroll.account(employment.id()).unpaid();
            long breachThreshold = employment.dailyWageMinor() > Long.MAX_VALUE / 7L
                    ? Long.MAX_VALUE : employment.dailyWageMinor() * 7L;
            if (arrears > 0L && arrears >= breachThreshold) {
                EconomicContractBridge.employmentBreach(server, employment.id(), arrears, server.overworld().getGameTime());
            }
        }
    }

    /** Pays as much of each employee's recorded wage claim as liquidation assets allow. */
    public static long payOutstandingForCompany(MinecraftServer server, Company company) {
        if (server == null || company == null) return 0L;
        LaborMarketSavedData labor = LaborMarketSavedData.get(server);
        LaborPayrollSavedData payroll = LaborPayrollSavedData.get(server);
        long paidTotal = 0L;
        for (EmploymentRecord employment : labor.employments()) {
            if (!company.companyId().equals(employment.employerId())) continue;
            long unpaid = payroll.account(employment.id()).unpaid();
            if (unpaid <= 0L) continue;
            long available = Math.max(0L, company.treasuryOf(Currencies.USD.id()));
            long availableMinor = available >= Long.MAX_VALUE / Money.MINOR_UNITS_PER_UNIT
                    ? Long.MAX_VALUE : available * Money.MINOR_UNITS_PER_UNIT;
            long amountMinor = Math.min(unpaid, availableMinor);
            long amountMajor = amountMinor / Money.MINOR_UNITS_PER_UNIT;
            if (amountMajor <= 0L) continue;
            amountMinor = amountMajor * Money.MINOR_UNITS_PER_UNIT;
            long householdAmount = ExchangeRates.convert(amountMinor, Currencies.USD, Config.defaultCurrency());
            String source = "liquidation-labor-payroll:" + employment.id() + ":" + unpaid;
            boolean debited = CompanyLedgerSavedData.get(server).entries(company.companyId()).stream()
                    .anyMatch(entry -> entry.description() != null
                            && entry.description().contains("[source=" + source + "]"));
            if (!debited && !CompanyHelper.debitTreasuryNonOperatingOnce(server, company.companyId(),
                    Currencies.USD.id(), amountMajor, "liquidation_labor_payroll",
                    "Liquidation employee wage claim", source)) continue;
            boolean delivered = false;
            try {
                java.util.UUID worker = java.util.UUID.fromString(employment.workerId());
                MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
                String payout = "liquidation-labor-payout:" + employment.id() + ":" + unpaid;
                delivered = mailbox.hasCreditSource(payout)
                        || mailbox.creditMoneyOnce(worker, Currencies.USD.id(), amountMinor, payout);
                PopulationSavedData population = PopulationSavedData.get(server);
                delivered = delivered && (population.hasCreditedSource(payout + ":household")
                        || population.addCashOnce(worker.toString(), householdAmount, payout + ":household"));
            } catch (IllegalArgumentException npcWorker) {
                String payout = "liquidation-labor-payout:" + employment.id() + ":" + unpaid;
                PopulationSavedData population = PopulationSavedData.get(server);
                delivered = population.hasCreditedSource(payout)
                        || population.addCashOnce(employment.workerId(), householdAmount, payout);
            }
            if (delivered) {
                payroll.settle(employment.id(), server.overworld().getGameTime() / 24000L,
                        unpaid - amountMinor);
                paidTotal = add(paidTotal, amountMinor);
            }
        }
        return paidTotal;
    }

    private static long toMinor(long major) {
        return major <= 0L ? 0L : major >= Long.MAX_VALUE / Money.MINOR_UNITS_PER_UNIT
                ? Long.MAX_VALUE : major * Money.MINOR_UNITS_PER_UNIT;
    }
    private static long withholdingTax(long grossMinor) {
        if (grossMinor <= 1L || Config.INCOME_TAX_RATE.get() <= 0.0) return 0L;
        double calculated = Math.floor(grossMinor * Config.INCOME_TAX_RATE.get());
        if (!Double.isFinite(calculated) || calculated >= grossMinor) return grossMinor - 1L;
        return Math.max(0L, (long) calculated);
    }

    private static long add(long left, long right) {
        try { return Math.addExact(left, right); }
        catch (ArithmeticException ignored) { return Long.MAX_VALUE; }
    }
}
