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
        long windowStart = lookbackTicks > 0L && currentTick > Long.MIN_VALUE + lookbackTicks
                ? currentTick - lookbackTicks : Long.MIN_VALUE;
        long cashFlow = 0L;
        boolean history = false;
        for (CompanyLedgerEntry entry : entries) {
            if (entry == null || entry.timestamp() < windowStart || entry.timestamp() > currentTick
                    || !isOperating(entry)) continue;
            history = true;
            cashFlow = addSaturated(cashFlow, entry.amount());
        }
        long totalDebt = addSaturated(Math.max(0L, existingDebt), Math.max(0L, requestedDebt));
        long supportedDebt = cashFlow > 0L ? multiplySaturated(cashFlow, 3L) : 0L;
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

    private static long addSaturated(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException e) {
            return right >= 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
    }

    private static long multiplySaturated(long value, long factor) {
        try {
            return Math.multiplyExact(value, factor);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }
}
