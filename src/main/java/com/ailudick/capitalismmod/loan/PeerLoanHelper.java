package com.ailudick.capitalismmod.loan;

import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Shared repayment operations for player-to-player loans. */
public final class PeerLoanHelper {
    private PeerLoanHelper() {}

    public static boolean repay(ServerPlayer borrower, String loanId, Long requestedAmount) {
        PeerLoanSavedData data = PeerLoanSavedData.get(borrower.getServer());
        PeerLoan loan = data.findLoan(loanId);
        if (loan == null || !loan.borrower().equals(borrower.getUUID())) {
            borrower.sendSystemMessage(Component.translatable("command.capitalismmod.loan_not_found"));
            return false;
        }
        Currency currency = Currencies.byId(loan.currencyId());
        if (currency == null || loan.principal() <= 0L) {
            borrower.sendSystemMessage(Component.translatable("command.capitalismmod.loan_not_found"));
            return false;
        }
        long interest = loan.interestDue();
        long total;
        try {
            total = Math.addExact(loan.principal(), interest);
        } catch (ArithmeticException exception) {
            borrower.sendSystemMessage(Component.translatable("command.capitalismmod.insufficient"));
            return false;
        }
        long payment = requestedAmount == null ? total : requestedAmount;
        if (payment <= 0L || payment > total) {
            borrower.sendSystemMessage(Component.translatable("command.capitalismmod.insufficient"));
            return false;
        }
        PeerLoanPaymentAllocation allocation = PeerLoanPaymentAllocation.forAmount(loan, payment).orElse(null);
        if (allocation == null) {
            borrower.sendSystemMessage(Component.translatable("command.capitalismmod.insufficient"));
            return false;
        }
        long totalMinor = Money.toMinor(payment);
        if (totalMinor <= 0L || !EconomyHelper.tryPay(borrower, currency, totalMinor)) {
            borrower.sendSystemMessage(Component.translatable("command.capitalismmod.insufficient"));
            return false;
        }
        ServerPlayer lender = borrower.getServer().getPlayerList().getPlayer(loan.lender());
        if (lender != null) {
            EconomyHelper.giveMoney(lender, currency, totalMinor);
        } else {
            MarketMailboxSavedData.get(borrower.getServer()).creditMoney(loan.lender(), currency.id(), totalMinor);
        }
        if (payment == total) {
            data.removeLoan(loanId);
        } else {
            long paidInterest = loan.interestPaid() > Long.MAX_VALUE - allocation.interestPayment()
                    ? Long.MAX_VALUE : loan.interestPaid() + allocation.interestPayment();
            PeerLoan updated = loan.withInterestPaid(paidInterest)
                    .withPrincipal(allocation.remainingPrincipal());
            if (allocation.principalPayment() > 0L) {
                long elapsed = Math.max(0L, (long) loan.totalDays() - loan.daysRemaining());
                updated = updated.withInterestAccrualState(loan.totalInterestAccrued(), elapsed);
            }
            data.replaceLoan(updated);
        }
        PeerLoanPaymentSavedData.get(borrower.getServer()).append(new PeerLoanPaymentSavedData.Payment(
                loan.id(), loan.lender(), loan.borrower(), borrower.getServer().overworld().getGameTime(),
                payment, allocation.interestPayment(), allocation.principalPayment(),
                allocation.remainingPrincipal(), loan.daysRemaining(), loan.isOverdue()));
        borrower.sendSystemMessage(Component.translatable("command.capitalismmod.loan_repaid",
                payment, Component.translatable(currency.nameKey())));
        return true;
    }
}
