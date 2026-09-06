package com.ailudick.capitalismmod.loan;

import java.util.List;

/** Estimates annual debt service for company underwriting without adding persisted fields. */
public record CompanyDebtServiceAssessment(double annualOperatingCashFlow,
                                           double annualDebtService,
                                           double coverageRatio,
                                           boolean approved) {
    public static CompanyDebtServiceAssessment evaluate(long recentOperatingCashFlow,
                                                        List<CompanyLoan> existingLoans,
                                                        long requestedPrincipal, int requestedDays,
                                                        double requestedRatePerYear,
                                                        boolean hasOperatingHistory) {
        return evaluate(recentOperatingCashFlow, existingLoans, requestedPrincipal, requestedDays,
                requestedRatePerYear, hasOperatingHistory, 1.25, 90L);
    }

    public static CompanyDebtServiceAssessment evaluate(long recentOperatingCashFlow,
                                                        List<CompanyLoan> existingLoans,
                                                        long requestedPrincipal, int requestedDays,
                                                        double requestedRatePerYear,
                                                        boolean hasOperatingHistory,
                                                        double minimumCoverageRatio) {
        return evaluate(recentOperatingCashFlow, existingLoans, requestedPrincipal, requestedDays,
                requestedRatePerYear, hasOperatingHistory, minimumCoverageRatio, 90L);
    }

    /** Evaluates debt service using the actual period represented by the cash flow. */
    public static CompanyDebtServiceAssessment evaluate(long recentOperatingCashFlow,
                                                        List<CompanyLoan> existingLoans,
                                                        long requestedPrincipal, int requestedDays,
                                                        double requestedRatePerYear,
                                                        boolean hasOperatingHistory,
                                                        double minimumCoverageRatio,
                                                        long recentPeriodDays) {
        double annualCashFlow = annualizeCashFlow(recentOperatingCashFlow, recentPeriodDays);
        double debtService = 0.0;
        for (CompanyLoan loan : existingLoans) {
            debtService += annualizedService(loan.principal(), loan.totalDays(), loan.ratePerYear());
        }
        debtService += annualizedService(requestedPrincipal, requestedDays, requestedRatePerYear);
        double ratio = debtService <= 0.0 ? Double.POSITIVE_INFINITY : annualCashFlow / debtService;
        boolean approved = !hasOperatingHistory
                || (annualCashFlow > 0.0 && debtService > 0.0
                && ratio >= Math.max(0.0, minimumCoverageRatio));
        return new CompanyDebtServiceAssessment(annualCashFlow, debtService, ratio, approved);
    }

    private static double annualizeCashFlow(long cashFlow, long periodDays) {
        if (cashFlow <= 0L || periodDays <= 0L) return 0.0;
        double annual = (double) cashFlow * 365.0D / (double) periodDays;
        return Double.isFinite(annual) ? annual : Double.MAX_VALUE;
    }

    /** Automated underwriting does not extend new credit while an existing loan is overdue. */
    public static boolean hasOverdueLoan(List<CompanyLoan> loans) {
        return loans != null && loans.stream().anyMatch(loan -> loan != null && loan.isOverdue());
    }

    private static double annualizedService(long principal, int days, double ratePerYear) {
        if (principal <= 0L || days <= 0 || !Double.isFinite(ratePerYear) || ratePerYear < 0.0) return 0.0;
        // Conservative balloon-loan approximation: principal due at maturity plus one year of interest.
        double service = principal * (365.0 / days) + principal * ratePerYear;
        return Double.isFinite(service) ? service : Double.MAX_VALUE;
    }
}
