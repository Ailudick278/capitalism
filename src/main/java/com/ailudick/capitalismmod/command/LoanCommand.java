package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.loan.PeerLoan;
import com.ailudick.capitalismmod.loan.PeerLoanHelper;
import com.ailudick.capitalismmod.loan.PeerLoanPaymentAllocation;
import com.ailudick.capitalismmod.loan.PeerLoanPaymentSavedData;
import com.ailudick.capitalismmod.loan.PeerLoanSavedData;
import com.ailudick.capitalismmod.loan.PeerLoanOriginationIntentSavedData;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Peer-to-peer loans: /lend, /repay, /loans.
 */
public class LoanCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lend")
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("currency", StringArgumentType.word())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .then(Commands.argument("days", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("rate", DoubleArgumentType.doubleArg(0.0))
                                                        .executes(ctx -> {
                                                            ServerPlayer lender = ctx.getSource().getPlayerOrException();
                                                            ServerPlayer borrower = EntityArgument.getPlayer(ctx, "target");
                                                            String currencyId = StringArgumentType.getString(ctx, "currency");
                                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                                            int days = IntegerArgumentType.getInteger(ctx, "days");
                                                            double rate = DoubleArgumentType.getDouble(ctx, "rate");
                                                            return lend(lender, borrower, currencyId, amount, days, rate);
                                                        })))))));

        dispatcher.register(Commands.literal("repay")
                .then(Commands.argument("loanId", StringArgumentType.word())
                        .executes(ctx -> PeerLoanHelper.repay(ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "loanId"), null) ? 1 : 0)
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> PeerLoanHelper.repay(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "loanId"),
                                        LongArgumentType.getLong(ctx, "amount")) ? 1 : 0))));

        dispatcher.register(Commands.literal("loans").executes(ctx -> list(ctx.getSource().getPlayerOrException())));
        dispatcher.register(Commands.literal("loanhistory")
                .then(Commands.argument("loanId", StringArgumentType.word())
                        .executes(ctx -> history(ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "loanId")))));
    }

    private static int lend(ServerPlayer lender, ServerPlayer borrower, String currencyId, long amount, int days, double ratePercent) {
        long amountMinor = Money.toMinor(amount);
        if (!Currencies.exists(currencyId) || amountMinor <= 0 || !Double.isFinite(ratePercent) || ratePercent < 0) {
            lender.sendSystemMessage(Component.translatable("command.capitalismmod.unknown_currency"));
            return 0;
        }
        Currency currency = Currencies.byId(currencyId);
        String loanId = UUID.randomUUID().toString();
        PeerLoanOriginationIntentSavedData intents = PeerLoanOriginationIntentSavedData.get(lender.getServer());
        intents.add(new PeerLoanOriginationIntentSavedData.Intent(loanId, lender.getUUID(), borrower.getUUID(),
                currencyId, amount, ratePercent / 100.0, days, false));
        String paymentSource = "peer-loan-origination:" + loanId;
        if (!EconomyHelper.tryPayWithReference(lender, currency, amountMinor, paymentSource)) {
            intents.remove(loanId);
            lender.sendSystemMessage(Component.translatable("command.capitalismmod.insufficient"));
            return 0;
        }
        intents.markPaid(loanId);
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(lender.getServer());
        String disbursementSource = "peer-loan-disbursement:" + loanId;
        if (!mailbox.hasCreditSource(disbursementSource)
                && !mailbox.creditMoneyOnce(borrower.getUUID(), currencyId, amountMinor, disbursementSource)) {
            lender.sendSystemMessage(Component.literal("放款已进入恢复队列，请勿重复操作。"));
            return 1;
        }
        mailbox.redeem(borrower);
        PeerLoanSavedData.get(lender.getServer()).addLoan(new PeerLoan(
                loanId, lender.getUUID(), borrower.getUUID(),
                currencyId, amount, ratePercent / 100.0, days, days));
        intents.remove(loanId);
        lender.sendSystemMessage(Component.translatable("command.capitalismmod.loan_made",
                amount, Component.translatable(currency.nameKey()), borrower.getDisplayName()));
        borrower.sendSystemMessage(Component.translatable("command.capitalismmod.loan_received",
                amount, Component.translatable(currency.nameKey()), lender.getDisplayName()));
        return 1;
    }

    private static int repay(ServerPlayer borrower, String loanId, Long requestedAmount) {
        return PeerLoanHelper.repay(borrower, loanId, requestedAmount) ? 1 : 0;
    }

    private static int list(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("command.capitalismmod.loan_list_title"));
        for (PeerLoan loan : PeerLoanSavedData.get(player.getServer()).loans()) {
            if (loan.lender().equals(player.getUUID()) || loan.borrower().equals(player.getUUID())) {
                boolean isLender = loan.lender().equals(player.getUUID());
                player.sendSystemMessage(Component.literal(
                        loan.id().substring(0, Math.min(8, loan.id().length())) + " "
                                + (isLender ? "→" : "←") + " $" + loan.principal() + " "
                                + loan.currencyId() + " " + loan.daysRemaining() + "d"));
            }
        }
        return 1;
    }

    private static int history(ServerPlayer player, String loanId) {
        PeerLoan loan = PeerLoanSavedData.get(player.getServer()).findLoan(loanId);
        var records = PeerLoanPaymentSavedData.get(player.getServer()).forLoan(loanId);
        boolean participant = loan != null && (loan.lender().equals(player.getUUID())
                || loan.borrower().equals(player.getUUID()));
        if (!participant) {
            for (var record : records) {
                if (record.lender().equals(player.getUUID()) || record.borrower().equals(player.getUUID())) {
                    participant = true;
                    break;
                }
            }
        }
        if (!participant) {
            player.sendSystemMessage(Component.translatable("command.capitalismmod.loan_not_found"));
            return 0;
        }
        player.sendSystemMessage(Component.literal("贷款还款记录 " + loanId.substring(0, Math.min(8, loanId.length()))));
        if (records.isEmpty()) {
            player.sendSystemMessage(Component.literal("暂无还款记录"));
            return 1;
        }
        int start = Math.max(0, records.size() - 10);
        for (int i = start; i < records.size(); i++) {
            var record = records.get(i);
            player.sendSystemMessage(Component.literal(
                    PerpetualCalendar.formatMinecraftTicks(record.timestamp())
                            + " total=" + record.total()
                            + " interest=" + record.interest()
                            + " principal=" + record.principal()
                            + " remaining=" + record.remainingPrincipal()
                            + (record.overdue() ? " overdue" : "")));
        }
        return 1;
    }
}
