package com.ailudick.capitalismmod.loan;

import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.economy.EconomyLogSavedData;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

/** Shared repayment operations for player-to-player loans. */
public final class PeerLoanHelper {
    private PeerLoanHelper() {}

    /** Recovers peer-loan disbursements interrupted before the loan record was saved. */
    public static int recoverOriginationIntents(MinecraftServer server) {
        if (server == null) return 0;
        PeerLoanSavedData loans = PeerLoanSavedData.get(server);
        PeerLoanOriginationIntentSavedData intents = PeerLoanOriginationIntentSavedData.get(server);
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
        int recovered = 0;
        for (PeerLoanOriginationIntentSavedData.Intent intent : intents.intents()) {
            if (loans.findLoan(intent.loanId()) != null) {
                intents.remove(intent.loanId());
                recovered++;
                continue;
            }
            ServerPlayer lender = server.getPlayerList().getPlayer(intent.lender());
            if (!intent.paid()) {
                if (lender == null) continue;
                Currency currency = Currencies.byId(intent.currencyId());
                if (currency == null) continue;
                long minor = Money.toMinor(intent.principal());
                if (minor <= 0L || !EconomyHelper.tryPayWithReference(lender, currency, minor,
                        "peer-loan-origination:" + intent.loanId())) continue;
                intents.markPaid(intent.loanId());
            }
            long minor = Money.toMinor(intent.principal());
            String disbursementSource = "peer-loan-disbursement:" + intent.loanId();
            if (!mailbox.hasCreditSource(disbursementSource)
                    && !mailbox.creditMoneyOnce(intent.borrower(), intent.currencyId(), minor, disbursementSource)) continue;
            ServerPlayer borrower = server.getPlayerList().getPlayer(intent.borrower());
            if (borrower != null) mailbox.redeem(borrower);
            loans.addLoan(new PeerLoan(intent.loanId(), intent.lender(), intent.borrower(), intent.currencyId(),
                    intent.principal(), intent.ratePerYear(), intent.days(), intent.days()));
            intents.remove(intent.loanId());
            recovered++;
        }
        return recovered;
    }

    public static boolean repay(ServerPlayer borrower, String loanId, Long requestedAmount) {
        PeerLoanSavedData data = PeerLoanSavedData.get(borrower.getServer());
        PeerLoanPaymentSavedData paymentData = PeerLoanPaymentSavedData.get(borrower.getServer());
        for (PeerLoanPaymentSavedData.Payment previous : paymentData.forLoan(loanId)) {
            if (!borrower.getUUID().equals(previous.borrower())) continue;
            boolean sameRequestedPayment = requestedAmount != null && previous.total() == requestedAmount;
            boolean completedFullPayment = requestedAmount == null && previous.remainingPrincipal() <= 0L;
            if (sameRequestedPayment || completedFullPayment) {
                reconcileLoanState(borrower.getServer(), previous);
                creditLender(borrower.getServer(), previous);
                Currency previousCurrency = Currencies.byId(previous.currencyId());
                borrower.sendSystemMessage(Component.translatable("command.capitalismmod.loan_repaid",
                        previous.total(), previousCurrency == null
                                ? Component.literal(previous.currencyId())
                                : Component.translatable(previousCurrency.nameKey())));
                return true;
            }
        }
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
        PeerLoanPaymentSavedData.Payment receipt = new PeerLoanPaymentSavedData.Payment(
                loan.id(), loan.lender(), loan.borrower(), currency.id(), borrower.getServer().overworld().getGameTime(),
                payment, allocation.interestPayment(), allocation.principalPayment(),
                allocation.remainingPrincipal(), loan.daysRemaining(), loan.isOverdue());
        paymentData.append(receipt);
        reconcileLoanState(borrower.getServer(), receipt);
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
            reconcileLoanState(server, payment);
            if (creditLender(server, payment)) recovered++;
        }
        return recovered;
    }

    /** Applies a persisted payment's loan-side mutation exactly once after a crash. */
    private static void reconcileLoanState(net.minecraft.server.MinecraftServer server,
                                           PeerLoanPaymentSavedData.Payment payment) {
        PeerLoanSavedData loans = PeerLoanSavedData.get(server);
        PeerLoan loan = loans.findLoan(payment.loanId());
        if (loan == null) return;
        if (payment.remainingPrincipal() <= 0L) {
            if (loan.principal() == payment.principal()) loans.removeLoan(payment.loanId());
            return;
        }
        if (loan.principal() == payment.remainingPrincipal()) return;
        long expectedBefore = payment.remainingPrincipal() + payment.principal();
        if (loan.principal() != expectedBefore) return;
        long paidInterest = loan.interestPaid() > Long.MAX_VALUE - payment.interest()
                ? Long.MAX_VALUE : loan.interestPaid() + payment.interest();
        PeerLoan updated = loan.withInterestPaid(paidInterest)
                .withPrincipal(payment.remainingPrincipal());
        if (payment.principal() > 0L) {
            long elapsed = Math.max(0L, (long) loan.totalDays() - loan.daysRemaining());
            updated = updated.withInterestAccrualState(loan.totalInterestAccrued(), elapsed);
        }
        loans.replaceLoan(updated);
    }
}
