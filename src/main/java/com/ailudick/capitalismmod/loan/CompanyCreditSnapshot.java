package com.ailudick.capitalismmod.loan;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyLedgerSavedData;
import net.minecraft.server.MinecraftServer;

/**
 * Read-only explanation of the existing company-loan underwriting inputs.
 * It is deliberately not persisted and does not grant credit by itself.
 */
public record CompanyCreditSnapshot(long lookbackDays, long operatingCashFlow,
                                    long existingDebt, long capitalDebtLimit,
                                    long cashFlowDebtLimit, long remainingDebtCapacity,
                                    double annualDebtService, double coverageRatio,
                                    boolean hasOperatingHistory, boolean hasOverdueLoan,
                                    int paymentCount, int onTimePayments, int overduePayments,
                                    int paymentBehaviorScore) {
    public static CompanyCreditSnapshot from(MinecraftServer server, Company company, long lookbackDays) {
        long days = Math.max(1L, Math.min(360L, lookbackDays));
        if (server == null || company == null) {
            return new CompanyCreditSnapshot(days, 0L, 0L, 0L, 0L, 0L,
                    0.0, Double.POSITIVE_INFINITY, false, false, 0, 0, 0, 0);
        }
        long now = server.overworld().getGameTime();
        CompanyCreditAssessmentInputs inputs = CompanyCreditAssessmentInputs.from(server, company);
        CompanyCashFlowAssessment cash = CompanyCashFlowAssessment.evaluate(
                CompanyLedgerSavedData.get(server).entries(company.companyId()), now,
                PerpetualCalendar.ticksForDays(days), inputs.existingDebt(), 0L);
        CompanyDebtServiceAssessment debtService = CompanyDebtServiceAssessment.evaluate(
                cash.operatingCashFlow(), CompanyLoanSavedData.get(server).forCompany(company.companyId()),
                0L, 1, 0.0, cash.hasOperatingHistory());
        long capitalLimit = multiplySaturated(Math.max(0L, company.registeredCapital()),
                Config.MAX_COMPANY_DEBT_MULTIPLE.get());
        long cashLimit = Math.max(0L, cash.maximumSupportedDebt());
        long totalLimit = cash.hasOperatingHistory() ? Math.min(capitalLimit, cashLimit) : capitalLimit;
        long remaining = totalLimit > inputs.existingDebt() ? totalLimit - inputs.existingDebt() : 0L;
        boolean overdue = CompanyDebtServiceAssessment.hasOverdueLoan(
                CompanyLoanSavedData.get(server).forCompany(company.companyId()));
        CompanyCreditBehavior behavior = CompanyCreditBehavior.from(server, company.companyId());
        return new CompanyCreditSnapshot(days, cash.operatingCashFlow(), inputs.existingDebt(),
                capitalLimit, cashLimit, remaining, debtService.annualDebtService(),
                debtService.coverageRatio(), cash.hasOperatingHistory(), overdue,
                behavior.payments(), behavior.onTimePayments(), behavior.overduePayments(), behavior.score());
    }

    private static long multiplySaturated(long value, long factor) {
        try {
            return Math.multiplyExact(value, factor);
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private record CompanyCreditAssessmentInputs(long existingDebt) {
        private static CompanyCreditAssessmentInputs from(MinecraftServer server, Company company) {
            long debt = 0L;
            for (CompanyLoan loan : CompanyLoanSavedData.get(server).forCompany(company.companyId())) {
                try {
                    debt = Math.addExact(debt, Math.max(0L, loan.principal()));
                } catch (ArithmeticException ignored) {
                    debt = Long.MAX_VALUE;
                    break;
                }
            }
            return new CompanyCreditAssessmentInputs(debt);
        }
    }
}
