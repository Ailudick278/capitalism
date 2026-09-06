package com.ailudick.capitalismmod.loan;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanyLedgerEntry;
import com.ailudick.capitalismmod.company.CompanyLedgerSavedData;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.government.GovernmentPolicySavedData;
import com.ailudick.capitalismmod.government.MonetaryPolicyEconomics;
import com.ailudick.capitalismmod.risk.FinancialRiskPolicy;
import com.ailudick.capitalismmod.risk.FinancialRiskSavedData;
import com.ailudick.capitalismmod.util.EconomyMath;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Company financing operations. Principal is a liability, never operating revenue. */
public final class CompanyLoanHelper {
    private CompanyLoanHelper() {}

    public static String borrow(Player player, String companyName, long amount, int days, double ratePercent) {
        MinecraftServer server = player.getServer();
        Company company = CompanyHelper.getCompany(player, companyName);
        if (server == null || company == null || amount <= 0L || days <= 0
                || days > Config.MAX_COMPANY_LOAN_TERM_DAYS.get()
                || !Double.isFinite(ratePercent) || ratePercent < 0.0 || ratePercent > 100.0) return null;
        double effectiveRate = MonetaryPolicyEconomics.adjustedAnnualRate(ratePercent / 100.0,
                GovernmentPolicySavedData.get(server).policyRateBasisPoints());
        var risk = FinancialRiskSavedData.get(server).latest();
        int overdueShare = risk == null ? 0 : risk.overdueShareBasisPoints();
        if (!FinancialRiskPolicy.newCompanyCreditAllowed(overdueShare)) return null;
        long maximumDebt = EconomyMath.multiply(company.registeredCapital(), Config.MAX_COMPANY_DEBT_MULTIPLE.get());
        if (maximumDebt < 0L) return null;
        maximumDebt = (long) Math.floor(maximumDebt * FinancialRiskPolicy.creditMultiplier(overdueShare));
        List<CompanyLoan> existingLoans = CompanyLoanSavedData.get(server).forCompany(company.companyId());
        if (CompanyDebtServiceAssessment.hasOverdueLoan(existingLoans)) return null;
        long existingDebt = 0L;
        for (CompanyLoan loan : existingLoans) {
            existingDebt = EconomyMath.add(existingDebt, loan.principal());
            if (existingDebt < 0L) return null;
        }
        long newDebt = EconomyMath.add(existingDebt, amount);
        if (newDebt < 0L || newDebt > maximumDebt) return null;
        long lookbackDays = 90L;
        long lookback = PerpetualCalendar.ticksForDays(lookbackDays);
        CompanyCashFlowAssessment cashFlow = CompanyCashFlowAssessment.evaluate(
                CompanyLedgerSavedData.get(server).entries(company.companyId()),
                server.overworld().getGameTime(), lookback, existingDebt, amount,
                Config.COMPANY_LOAN_CASH_FLOW_DEBT_MULTIPLE.get());
        if (!cashFlow.approved()) return null;
        CompanyDebtServiceAssessment debtService = CompanyDebtServiceAssessment.evaluate(
                cashFlow.operatingCashFlow(), existingLoans,
                amount, days, effectiveRate, cashFlow.hasOperatingHistory(),
                Config.COMPANY_LOAN_MIN_COVERAGE_RATIO.get(), lookbackDays);
        if (!debtService.approved()) return null;
        String source = "company-loan:" + company.companyId() + ":" + existingDebt + ":" + amount
                + ":" + days + ":" + Double.doubleToLongBits(effectiveRate);
        if (!CompanyHelper.creditTreasuryNonOperatingOnce(server, company.companyId(), Currencies.USD.id(), amount,
                "loan_proceeds", "Company loan principal received", source)) return null;
        String id = source;
        CompanyLoanSavedData loanData = CompanyLoanSavedData.get(server);
        if (loanData.find(id) == null) {
            loanData.add(new CompanyLoan(id, company.companyId(),
                    Currencies.USD.id(), amount, effectiveRate, days, days, 0L));
        }
        return id;
    }

    public static boolean repay(Player player, String companyName, String loanId, Long requestedAmount) {
        MinecraftServer server = player.getServer();
        Company company = CompanyHelper.getCompany(player, companyName);
        if (server == null || company == null) return false;
        CompanyLoanSavedData data = CompanyLoanSavedData.get(server);
        CompanyLoan loan = data.find(loanId);
        if (loan == null || !loan.companyId().equals(company.companyId()) || !Currencies.exists(loan.currencyId())) return false;
        long total = EconomyMath.add(loan.principal(), loan.interestDue());
        if (total < 0L) return false;
        long payment = requestedAmount == null ? total : requestedAmount;
        var allocation = CompanyLoanPaymentAllocation.forAmount(loan, payment).orElse(null);
        if (allocation == null) return false;
        long interestPayment = allocation.interestPayment();
        long principalPayment = allocation.principalPayment();
        String debitSource = repaymentSource(loan, payment);
        if (!CompanyHelper.debitTreasuryNonOperatingOnce(server, company.companyId(), loan.currencyId(), payment,
                "loan_repayment", "Company loan repayment", debitSource)) return false;
        if (payment == total) {
            data.remove(loan.id());
        } else {
            CompanyLoan updated = loan.withInterestPaid(EconomyMath.add(loan.interestPaid(), interestPayment))
                    .withPrincipal(allocation.remainingPrincipal());
            if (principalPayment > 0L) {
                long elapsed = Math.max(0L, (long) loan.totalDays() - loan.daysRemaining());
                updated = updated.withInterestAccrualState(loan.totalInterestAccrued(), elapsed);
            }
            data.replace(updated);
        }
        if (interestPayment > 0L) {
            Company current = CompanySavedData.get(server).get(company.companyId());
            if (current != null) {
                long occurredAt = server.overworld().getGameTime();
                CompanyHelper.recordTaxableExpense(server, current,
                        "loan_interest:" + loan.id() + ":" + occurredAt,
                        interestPayment, loan.currencyId(), occurredAt);
                CompanyLedgerSavedData.get(server).append(new CompanyLedgerEntry(
                        current.companyId(), occurredAt, "interest_expense", loan.currencyId(),
                        -interestPayment, current.treasuryOf(loan.currencyId()),
                        "Company loan interest expense"));
            }
        }
        CompanyLoanPaymentSavedData.get(server).append(new CompanyLoanPaymentSavedData.Payment(
                loan.id(), loan.companyId(), server.overworld().getGameTime(), payment,
                interestPayment, principalPayment, Math.max(0L, loan.principal() - principalPayment),
                loan.daysRemaining(), loan.isOverdue()));
        return true;
    }

    static String repaymentSource(CompanyLoan loan, long payment) {
        if (loan == null) return "";
        return "company-loan-repayment:" + loan.id() + ":" + loan.principal()
                + ":" + loan.interestDue() + ":" + payment;
    }

    /** Pays the current equal-payment estimate for a company loan. */
    public static boolean repayScheduled(Player player, String companyName, String loanId) {
        MinecraftServer server = player.getServer();
        Company company = CompanyHelper.getCompany(player, companyName);
        if (server == null || company == null) return false;
        CompanyLoan loan = CompanyLoanSavedData.get(server).find(loanId);
        if (loan == null || !loan.companyId().equals(company.companyId())) return false;
        long scheduled = loan.scheduledPayment();
        return scheduled > 0L && repay(player, companyName, loanId, scheduled);
    }
}
