package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.loan.PeerLoan;
import com.ailudick.capitalismmod.loan.PeerLoanSavedData;
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
                        .executes(ctx -> repay(ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "loanId")))));

        dispatcher.register(Commands.literal("loans").executes(ctx -> list(ctx.getSource().getPlayerOrException())));
    }

    private static int lend(ServerPlayer lender, ServerPlayer borrower, String currencyId, long amount, int days, double ratePercent) {
        if (!Currencies.exists(currencyId)) {
            lender.sendSystemMessage(Component.translatable("command.capitalismmod.unknown_currency"));
            return 0;
        }
        Currency currency = Currencies.byId(currencyId);
        if (!EconomyHelper.tryPay(lender, currency, Money.toMinor(amount))) {
            lender.sendSystemMessage(Component.translatable("command.capitalismmod.insufficient"));
            return 0;
        }
        EconomyHelper.giveMoney(borrower, currency, Money.toMinor(amount));
        PeerLoanSavedData.get(lender.getServer()).addLoan(new PeerLoan(
                UUID.randomUUID().toString(), lender.getUUID(), borrower.getUUID(),
                currencyId, amount, ratePercent / 100.0, days, days));
        lender.sendSystemMessage(Component.translatable("command.capitalismmod.loan_made",
                amount, Component.translatable(currency.nameKey()), borrower.getDisplayName()));
        borrower.sendSystemMessage(Component.translatable("command.capitalismmod.loan_received",
                amount, Component.translatable(currency.nameKey()), lender.getDisplayName()));
        return 1;
    }

    private static int repay(ServerPlayer borrower, String loanId) {
        PeerLoanSavedData data = PeerLoanSavedData.get(borrower.getServer());
        PeerLoan loan = data.findLoan(loanId);
        if (loan == null || !loan.borrower().equals(borrower.getUUID())) {
            borrower.sendSystemMessage(Component.translatable("command.capitalismmod.loan_not_found"));
            return 0;
        }
        Currency currency = Currencies.byId(loan.currencyId());
        long total = loan.principal() + loan.interestDue();
        if (!EconomyHelper.tryPay(borrower, currency, Money.toMinor(total))) {
            borrower.sendSystemMessage(Component.translatable("command.capitalismmod.insufficient"));
            return 0;
        }
        ServerPlayer lender = borrower.getServer().getPlayerList().getPlayer(loan.lender());
        if (lender != null) {
            EconomyHelper.giveMoney(lender, currency, Money.toMinor(total));
        } else {
            MarketMailboxSavedData.get(borrower.getServer()).creditMoney(loan.lender(), currency.id(), Money.toMinor(total));
        }
        data.removeLoan(loanId);
        borrower.sendSystemMessage(Component.translatable("command.capitalismmod.loan_repaid",
                total, Component.translatable(currency.nameKey())));
        return 1;
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
}
