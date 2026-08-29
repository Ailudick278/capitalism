package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BalanceCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("balance")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    for (Currency currency : Currencies.ALL) {
                        player.sendSystemMessage(Component.translatable("command.capitalismmod.balance_line",
                                Component.translatable(currency.nameKey()), Money.format(EconomyHelper.getBalance(player, currency))));
                    }
                    return 1;
                }));
    }
}
