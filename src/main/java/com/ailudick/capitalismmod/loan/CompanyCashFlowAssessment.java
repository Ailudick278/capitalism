package com.ailudick.capitalismmod.loan;

import com.ailudick.capitalismmod.company.CompanyLedgerEntry;

import java.util.List;

/**
 * Cash-flow based lending assessment for a company.
 * It deliberately derives from the existing ledger and stores no new state.
 */
public record CompanyCashFlowAssessment(long operatingCashFlow, long existingDebt,
                                        long requestedDebt, long maximumSupportedDebt,
                                        boolean hasOperatingHistory, boolean approved) {
    public static CompanyCashFlowAssessment evaluate(List<CompanyLedgerEntry> entries,
                                                      long currentTick, long lookbackTicks,
                                                      long existingDebt, long requestedDebt) {
        return evaluate(entries, currentTick, lookbackTicks, existingDebt, requestedDebt, 3.0);
    }

    /** Evaluates the same ledger with a server-defined cash-flow debt multiple. */
    public static CompanyCashFlowAssessment evaluate(List<CompanyLedgerEntry> entries,
                                                      long currentTick, long lookbackTicks,
                                                      long existingDebt, long requestedDebt,
                                                      double cashFlowDebtMultiple) {
        long windowStart = lookbackTicks > 0L && currentTick > Long.MIN_VALUE + lookbackTicks
                ? currentTick - lookbackTicks : Long.MIN_VALUE;
        long cashFlow = 0L;
        boolean history = false;
        for (CompanyLedgerEntry entry : entries) {
            if (entry == null || entry.timestamp() < windowStart || entry.timestamp() > currentTick
                    || !isOperating(entry)) continue;
            history = true;
            if (isCashFlow(entry)) cashFlow = addSaturated(cashFlow, entry.amount());
        }
        long totalDebt = addSaturated(Math.max(0L, existingDebt), Math.max(0L, requestedDebt));
        long supportedDebt = cashFlow > 0L ? multiplySaturated(cashFlow, cashFlowDebtMultiple) : 0L;
        boolean approved = !history || (cashFlow > 0L && totalDebt <= supportedDebt);
        return new CompanyCashFlowAssessment(cashFlow, Math.max(0L, existingDebt),
                Math.max(0L, requestedDebt), supportedDebt, history, approved);
    }

    private static boolean isOperating(CompanyLedgerEntry entry) {
        String type = entry.type() == null ? "" : entry.type();
        return !type.equals("loan_proceeds")
                && !type.equals("loan_repayment")
                && !type.equals("capital_contribution")
                && !type.equals("dividend_distribution")
                && !type.equals("owner_withdrawal")
                && !type.equals("equipment_purchase");
    }

    /** Excludes non-cash accounting entries from the lending cash-flow test. */
    private static boolean isCashFlow(CompanyLedgerEntry entry) {
        String type = entry.type() == null ? "" : entry.type();
        return !type.equals("cost_of_goods_sold")
                && !type.equals("inventory_loss")
                && !type.equals("accrued_expense");
    }

    private static long addSaturated(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException e) {
            return right >= 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
    }

    private static long multiplySaturated(long value, double factor) {
        if (value <= 0L || !Double.isFinite(factor) || factor <= 0.0) return 0L;
        double result = (double) value * factor;
        return !Double.isFinite(result) || result >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) result;
    }
}
