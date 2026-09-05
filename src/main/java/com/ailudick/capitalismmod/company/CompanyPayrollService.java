package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.util.EconomyMath;
import net.minecraft.server.MinecraftServer;

/** Pays contract wages daily and carries unpaid wages as a persistent liability. */
public final class CompanyPayrollService {
    private CompanyPayrollService() {
    }

    public static void settleDaily(MinecraftServer server, long settlementDay) {
        CompanyPayrollSavedData payroll = CompanyPayrollSavedData.get(server);
        for (Company company : CompanySavedData.get(server).companies().values()) {
            if (!CompanyLifecycleService.canOperate(server, company.companyId())
                    && !"SUSPENDED".equals(CompanyLifecycleService.status(server, company.companyId()))) continue;
            long wages = CompanyLaborSavedData.get(server).dailyWages(company.companyId());
            long unpaid = payroll.unpaid(company.companyId());
            long due = EconomyMath.add(unpaid, wages);
            if (due <= 0L) continue;
            Company current = CompanySavedData.get(server).get(company.companyId());
            long available = current == null ? 0L : Math.max(0L, current.treasuryOf(Currencies.USD.id()));
            long paid = Math.min(available, due);
            if (paid > 0L && !CompanyHelper.debitTreasury(server, company.companyId(), Currencies.USD.id(),
                    paid, "payroll", "每日员工工资")) paid = 0L;
            payroll.settle(company.companyId(), settlementDay, paid, due - paid);
        }
    }

    public static long payOutstanding(MinecraftServer server, Company company) {
        if (server == null || company == null) return 0L;
        CompanyPayrollSavedData payroll = CompanyPayrollSavedData.get(server);
        long unpaid = payroll.unpaid(company.companyId());
        if (unpaid <= 0L) return 0L;
        Company current = CompanySavedData.get(server).get(company.companyId());
        if (current == null) return 0L;
        long paid = Math.min(unpaid, Math.max(0L, current.treasuryOf(Currencies.USD.id())));
        if (paid > 0L && CompanyHelper.debitTreasury(server, company.companyId(), Currencies.USD.id(),
                paid, "liquidation_payroll", "清算优先清偿员工工资")) {
            payroll.settle(company.companyId(), server.overworld().getGameTime() / 24000L, paid, unpaid - paid);
            return paid;
        }
        return 0L;
    }
}
