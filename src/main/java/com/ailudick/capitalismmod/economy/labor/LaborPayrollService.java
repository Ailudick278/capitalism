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
            if (company == null) continue;
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
            if (paidMajor > 0L && (previousDebit != null || CompanyHelper.debitTreasuryNonOperatingOnce(server, company.companyId(), Currencies.USD.id(), paidMajor,
                    "employment_payroll", "Employment wage payment", source))) {
                try {
                    java.util.UUID workerId = java.util.UUID.fromString(employment.workerId());
                    MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
                    mailbox.creditMoneyOnce(workerId, Currencies.USD.id(), paid, source);
                    ServerPlayer worker = server.getPlayerList().getPlayer(workerId);
                    if (worker != null) mailbox.redeemMoneyOnly(worker);
                    PopulationSavedData.get(server).addCashOnce(workerId.toString(),
                            ExchangeRates.convert(paid, Currencies.USD, Config.defaultCurrency()), source + ":household");
                } catch (IllegalArgumentException npcWorker) {
                    PopulationSavedData.get(server).addCashOnce(employment.workerId(),
                            ExchangeRates.convert(paid, Currencies.USD, Config.defaultCurrency()), source);
                }
            } else paid = 0L;
            payroll.settle(employment.id(), day, due - paid);
            long arrears = payroll.account(employment.id()).unpaid();
            long breachThreshold = employment.dailyWageMinor() > Long.MAX_VALUE / 7L
                    ? Long.MAX_VALUE : employment.dailyWageMinor() * 7L;
            if (arrears > 0L && arrears >= breachThreshold) {
                EconomicContractBridge.employmentBreach(server, employment.id(), arrears, server.overworld().getGameTime());
            }
        }
    }

    private static long toMinor(long major) {
        return major <= 0L ? 0L : major >= Long.MAX_VALUE / Money.MINOR_UNITS_PER_UNIT
                ? Long.MAX_VALUE : major * Money.MINOR_UNITS_PER_UNIT;
    }
}
