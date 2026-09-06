package com.ailudick.capitalismmod.loan;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanyLedgerEntry;
import com.ailudick.capitalismmod.company.CompanyLedgerSavedData;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.util.EconomyMath;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/** Company financing operations. Principal is a liability, never operating revenue. */
public final class CompanyLoanHelper {
    private CompanyLoanHelper() {}

    public static String borrow(Player player, String companyName, long amount, int days, double ratePercent) {
        MinecraftServer server = player.getServer();
        Company company = CompanyHelper.getCompany(player, companyName);
        if (server == null || company == null || amount <= 0L || days <= 0 || days > 3650
                || !Double.isFinite(ratePercent) || ratePercent < 0.0 || ratePercent > 100.0) return null;
        long maximumDebt = EconomyMath.multiply(company.registeredCapital(), Config.MAX_COMPANY_DEBT_MULTIPLE.get());
        if (maximumDebt < 0L) return null;
        List<CompanyLoan> existingLoans = CompanyLoanSavedData.get(server).forCompany(company.companyId());
        if (CompanyDebtServiceAssessment.hasOverdueLoan(existingLoans)) return null;
        long existingDebt = 0L;
        for (CompanyLoan loan : existingLoans) {
            existingDebt = EconomyMath.add(existingDebt, loan.principal());
            if (existingDebt < 0L) return null;
        }
        long newDebt = EconomyMath.add(existingDebt, amount);
        if (newDebt < 0L || newDebt > maximumDebt) return null;
        long lookback = PerpetualCalendar.ticksForDays(90L);
        CompanyCashFlowAssessment cashFlow = CompanyCashFlowAssessment.evaluate(
                CompanyLedgerSavedData.get(server).entries(company.companyId()),
                server.overworld().getGameTime(), lookback, existingDebt, amount);
        if (!cashFlow.approved()) return null;
        CompanyDebtServiceAssessment debtService = CompanyDebtServiceAssessment.evaluate(
                cashFlow.operatingCashFlow(), existingLoans,
                amount, days, ratePercent / 100.0, cashFlow.hasOperatingHistory(),
                Config.COMPANY_LOAN_MIN_COVERAGE_RATIO.get());
        if (!debtService.approved()) return null;
        if (!CompanyHelper.creditTreasuryNonOperating(server, company.companyId(), Currencies.USD.id(), amount,
                "loan_proceeds", "Company loan principal received")) return null;
        String id = UUID.randomUUID().toString();
        CompanyLoanSavedData.get(server).add(new CompanyLoan(id, company.companyId(),
                Currencies.USD.id(), amount, ratePercent / 100.0, days, days, 0L));
        return id;
    }

    public static boolean repay(Player player, String companyName, String loanId, Long requestedAmount) {
        MinecraftServer server = player.getServer();
        Company company = CompanyHelper.getCompany(player, companyName);
        if (server == null || company == null) return false;
        CompanyLoanSavedData data = CompanyLoanSavedData.get(server);
        CompanyLoan loan = data.find(loanId);
        if (loan == null || !loan.companyId().equals(company.companyId()) || !Currencies.exists(loan.currencyId())) return false;
        long interest = loan.interestDue();
        long total = EconomyMath.add(loan.principal(), interest);
        if (total < 0L) return false;
        long payment = requestedAmount == null ? total : requestedAmount;
        if (payment <= 0L || payment > total) return false;
        long interestPayment = Math.min(payment, interest);
        long principalPayment = payment - interestPayment;
        if (principalPayment < 0L || principalPayment > loan.principal()) return false;
        if (!CompanyHelper.debitTreasuryNonOperating(server, company.companyId(), loan.currencyId(), payment,
                "loan_repayment", "Company loan repayment")) return false;
        if (payment == total) {
            data.remove(loan.id());
        } else {
            CompanyLoan updated = loan.withInterestPaid(EconomyMath.add(loan.interestPaid(), interestPayment))
                    .withPrincipal(loan.principal() - principalPayment);
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
