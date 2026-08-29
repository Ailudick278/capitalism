package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.ExchangeRateProvider;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Displays the current exchange rates (live or fallback).
 */
public class FxCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fx").executes(ctx -> run(ctx.getSource())));
    }

    private static int run(CommandSourceStack source) {
        String sourceName = ExchangeRateProvider.isLive() ? "live" : "fallback";
        source.sendSuccess(() -> Component.literal("[FX] " + sourceName), false);
        for (Currency currency : Currencies.ALL) {
            double cny = ExchangeRateProvider.effective(currency) / 100.0;
            source.sendSuccess(() -> Component.literal(currency.id().toUpperCase() + " 1 = " + String.format("%.2f", cny) + " CNY"), false);
        }
        return 1;
    }
}
