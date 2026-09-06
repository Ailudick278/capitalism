package com.ailudick.capitalismmod.economy.labor;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.company.CompanyPayrollService;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
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
            long due;
            try { due = Math.addExact(payroll.account(employment.id()).unpaid(), employment.dailyWageMinor()); }
            catch (ArithmeticException e) { due = Long.MAX_VALUE; }
            long contribution = CompanyPayrollService.employerContribution(employment.dailyWageMinor());
            long laborCost;
            try { laborCost = Math.addExact(employment.dailyWageMinor(), contribution); }
            catch (ArithmeticException e) { laborCost = Long.MAX_VALUE; }
            CompanyHelper.recordNonCashExpense(server, company, "labor_payroll:" + employment.id() + ":" + day,
                    laborCost, Currencies.USD.id(), "Employment gross wages and employer contribution");
            long available = Math.max(0L, company.treasuryOf(Currencies.USD.id()));
            long paid = Math.min(available, due);
            if (paid > 0L && CompanyHelper.debitTreasuryNonOperating(server, company.companyId(), Currencies.USD.id(), paid,
                    "employment_payroll", "Employment wage payment")) {
                ServerPlayer worker = server.getPlayerList().getPlayer(java.util.UUID.fromString(employment.workerId()));
                if (worker != null) EconomyHelper.giveMoney(worker, Currencies.USD, paid);
                else MarketMailboxSavedData.get(server).creditMoney(java.util.UUID.fromString(employment.workerId()), Currencies.USD.id(), paid);
            } else paid = 0L;
            payroll.settle(employment.id(), day, due - paid);
        }
    }
}
