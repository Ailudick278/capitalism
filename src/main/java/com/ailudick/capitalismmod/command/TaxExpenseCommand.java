package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.tax.TaxExpenseLedgerSavedData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Displays the player's recorded business expenses. */
public final class TaxExpenseCommand {
    private TaxExpenseCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("taxexpenses")
                .executes(context -> list(context.getSource())));
    }

    private static int list(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var expenses = TaxExpenseLedgerSavedData.get(player.getServer()).forTaxpayer(player.getUUID());
        if (expenses.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No recorded business expenses."), false);
            return 0;
        }
        for (var expense : expenses) {
            source.sendSuccess(() -> Component.literal(expense.category() + " | "
                    + expense.currencyId().toUpperCase() + " " + Money.format(expense.amount())
                    + " | deductible=" + expense.deductible() + " | " + expense.sourceId()), false);
        }
        return expenses.size();
    }
}
