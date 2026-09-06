package com.ailudick.capitalismmod.loan;

import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.economy.EconomyLogSavedData;
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
        String debitReference = "peer-loan-debit:" + loan.id() + ":" + payment + ":"
                + allocation.remainingPrincipal();
        boolean alreadyDebited = EconomyLogSavedData.get(borrower.getServer())
                .hasReference(borrower.getUUID(), debitReference);
        if (totalMinor <= 0L || (!alreadyDebited
                && !EconomyHelper.tryPayWithReference(borrower, currency, totalMinor, debitReference))) {
            borrower.sendSystemMessage(Component.translatable("command.capitalismmod.insufficient"));
            return false;
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
        PeerLoanPaymentSavedData.Payment receipt = new PeerLoanPaymentSavedData.Payment(
                loan.id(), loan.lender(), loan.borrower(), currency.id(), borrower.getServer().overworld().getGameTime(),
                payment, allocation.interestPayment(), allocation.principalPayment(),
                allocation.remainingPrincipal(), loan.daysRemaining(), loan.isOverdue());
        PeerLoanPaymentSavedData.get(borrower.getServer()).append(receipt);
        creditLender(borrower.getServer(), receipt);
        borrower.sendSystemMessage(Component.translatable("command.capitalismmod.loan_repaid",
                payment, Component.translatable(currency.nameKey())));
        return true;
    }

    /** Completes a durable lender credit for a previously recorded repayment. */
    public static boolean creditLender(net.minecraft.server.MinecraftServer server,
                                       PeerLoanPaymentSavedData.Payment payment) {
        if (server == null || payment == null || payment.currencyId().isBlank()) return false;
        String source = PeerLoanPaymentSavedData.payoutSource(payment);
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
        if (!mailbox.hasCreditSource(source)
                && !mailbox.creditMoneyOnce(payment.lender(), payment.currencyId(), Money.toMinor(payment.total()), source)) {
            return false;
        }
        ServerPlayer lender = server.getPlayerList().getPlayer(payment.lender());
        if (lender != null) mailbox.redeemMoneyOnly(lender);
        return true;
    }

    public static int recoverRecordedPayments(net.minecraft.server.MinecraftServer server) {
        int recovered = 0;
        for (PeerLoanPaymentSavedData.Payment payment : PeerLoanPaymentSavedData.get(server).forAll()) {
            if (creditLender(server, payment)) recovered++;
        }
        return recovered;
    }
}
