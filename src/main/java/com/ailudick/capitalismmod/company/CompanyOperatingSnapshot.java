package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.util.EconomyMath;
import net.minecraft.server.MinecraftServer;

/**
 * Read-only operating indicators for comparing companies without inventing a
 * universal company level. The cash-flow window is derived from the persisted
 * company ledger and therefore also works for offline companies.
 */
public record CompanyOperatingSnapshot(long lookbackDays, long revenue, long operatingExpenses,
                                       long operatingCashFlow, int activeWorkers, long dailyPayroll,
                                       int machineUnits, int parallelCapacity, long successfulBatches,
                                       long failedCycles, long assets, long equity) {
    public static CompanyOperatingSnapshot from(MinecraftServer server, Company company, long lookbackDays) {
        long days = Math.max(1L, Math.min(360L, lookbackDays));
        if (server == null || company == null) {
            return new CompanyOperatingSnapshot(days, 0L, 0L, 0L, 0, 0L, 0, 0,
                    0L, 0L, 0L, 0L);
        }

        long now = server.overworld().getGameTime();
        long window = PerpetualCalendar.ticksForDays(days);
        long start = now > Long.MIN_VALUE + window ? now - window : Long.MIN_VALUE;
        long revenue = 0L;
        long expenses = 0L;
        long cashFlow = 0L;
        for (CompanyLedgerEntry entry : CompanyLedgerSavedData.get(server).entries(company.companyId())) {
            if (entry == null || !Currencies.USD.id().equals(entry.currencyId())
                    || entry.timestamp() < start || entry.timestamp() > now || !isOperating(entry)) continue;
            cashFlow = addSignedSaturated(cashFlow, entry.amount());
            if (entry.amount() > 0L) revenue = addSaturated(revenue, entry.amount());
            if (entry.amount() < 0L) {
                long expense = entry.amount() == Long.MIN_VALUE ? Long.MAX_VALUE : -entry.amount();
                expenses = addSaturated(expenses, expense);
            }
        }

        CompanyLaborSavedData labor = CompanyLaborSavedData.get(server);
        int workers = labor.activeWorkers(company.companyId());
        long dailyPayroll = Math.max(0L, labor.dailyWages(company.companyId()));
        long machineTotal = 0L;
        for (CompanyEquipmentSavedData.Equipment equipment
                : CompanyEquipmentSavedData.get(server).all(company.companyId()).values()) {
            long count = Math.max(0, equipment.count());
            machineTotal = Math.min(Integer.MAX_VALUE, machineTotal + count);
        }
        int machineUnits = (int) machineTotal;
        int capacity = Math.max(0, CompanyHelper.parallelCapacity(server, company));
        CompanyProductionSavedData.ProductionState production =
                CompanyProductionSavedData.get(server).get(company.companyId());
        long successful = production == null ? 0L : Math.max(0L, production.successfulCycles());
        long failed = production == null ? 0L : Math.max(0L, production.failedCycles());
        CompanyFinancialSnapshot financial = CompanyFinancialSnapshot.from(server, company);
        return new CompanyOperatingSnapshot(days, revenue, expenses, cashFlow, workers, dailyPayroll,
                machineUnits, capacity, successful, failed, financial.assets(), financial.equity());
    }

    private static boolean isOperating(CompanyLedgerEntry entry) {
        String type = entry.type() == null ? "" : entry.type();
        return !type.equals("loan_proceeds")
                && !type.equals("loan_repayment")
                && !type.equals("capital_contribution")
                && !type.equals("dividend_distribution")
                && !type.equals("owner_withdrawal")
                && !type.equals("equipment_purchase")
                && !type.startsWith("liquidation_");
    }

    private static long addSaturated(long left, long right) {
        long result = EconomyMath.add(left, right);
        if (result >= 0L) return result;
        return right >= 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
    }

    private static long addSignedSaturated(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            return right >= 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
    }
}
