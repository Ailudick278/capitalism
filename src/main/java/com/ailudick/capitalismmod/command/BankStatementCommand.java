package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.bank.BankAccount;
import com.ailudick.capitalismmod.bank.BankAccountHelper;
import com.ailudick.capitalismmod.bank.BankTransaction;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.currency.Money;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Read-only account statement and transaction summary. */
public final class BankStatementCommand {
    private BankStatementCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bankstatement")
                .then(Commands.argument("account", StringArgumentType.word())
                        .executes(ctx -> statement(ctx.getSource(),
                                StringArgumentType.getString(ctx, "account"), 0))
                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 360))
                                .executes(ctx -> statement(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "account"),
                                        IntegerArgumentType.getInteger(ctx, "days"))))));
    }

    private static int statement(CommandSourceStack source, String accountId, int days) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        BankAccount account = BankAccountHelper.getAccount(player, accountId);
        if (account == null) {
            source.sendFailure(Component.literal("Bank account not found."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("=== Bank statement " + account.id()
                + (days > 0 ? " / last " + days + " days" : " / retained history") + " ==="), false);
        source.sendSuccess(() -> Component.literal("Account type: " + (account.credit() ? "credit" : "debit")), false);
        for (Map.Entry<String, Long> entry : account.balances().entrySet()) {
            source.sendSuccess(() -> Component.literal("Balance " + entry.getKey().toUpperCase()
                    + ": " + Money.format(entry.getValue())), false);
        }
        for (Map.Entry<String, Long> entry : account.debts().entrySet()) {
            if (entry.getValue() > 0L) {
                source.sendSuccess(() -> Component.literal("Debt " + entry.getKey().toUpperCase()
                        + ": " + Money.format(entry.getValue())), false);
            }
        }
        long now = player.level().getGameTime();
        long start = days > 0 ? now - PerpetualCalendar.ticksForDays(days) : Long.MIN_VALUE;
        List<BankTransaction> visible = account.transactions().stream()
                .filter(transaction -> days <= 0 || transaction.occurredAt() >= start)
                .collect(Collectors.toList());
        Map<String, long[]> summary = new HashMap<>();
        for (BankTransaction transaction : visible) {
            long[] totals = summary.computeIfAbsent(transaction.currencyId(), ignored -> new long[2]);
            if (transaction.amount() >= 0L) {
                totals[0] = addSaturated(totals[0], transaction.amount());
            } else {
                long debit = transaction.amount() == Long.MIN_VALUE
                        ? Long.MAX_VALUE
                        : -transaction.amount();
                totals[1] = addSaturated(totals[1], debit);
            }
        }
        for (Map.Entry<String, long[]> entry : summary.entrySet()) {
            long[] totals = entry.getValue();
            source.sendSuccess(() -> Component.literal("Period ledger " + entry.getKey().toUpperCase()
                    + " | credits " + Money.format(totals[0])
                    + " | debits " + Money.format(totals[1])), false);
        }
        source.sendSuccess(() -> Component.literal("Recent transactions (up to "
                + visible.size() + ")"), false);
        int first = Math.max(0, visible.size() - 10);
        for (int i = first; i < visible.size(); i++) {
            BankTransaction transaction = visible.get(i);
            String occurred = transaction.occurredAt() < 0L
                    ? "legacy"
                    : PerpetualCalendar.formatMinecraftTicks(transaction.occurredAt());
            source.sendSuccess(() -> Component.literal(occurred + " | " + transaction.type() + " | "
                    + transaction.currencyId().toUpperCase() + " "
                    + Money.format(transaction.amount()) + " | ref " + transaction.reference()
                    + " | counterparty " + transaction.counterparty()), false);
        }
        return 1;
    }

    private static long addSaturated(long left, long right) {
        if (left < 0L || right < 0L) return Long.MAX_VALUE;
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }
}
