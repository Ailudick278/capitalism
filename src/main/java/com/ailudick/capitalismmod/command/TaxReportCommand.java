package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.tax.TaxAnnualReport;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Player-facing annual tax report command. */
public final class TaxReportCommand {
    private TaxReportCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("taxreport").executes(context -> report(context.getSource())));
    }
    private static int report(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TaxAnnualReport report = TaxAnnualReport.calculate(player.getServer(), player.getUUID(), player.getServer().overworld().getGameTime());
        source.sendSuccess(() -> Component.literal("=== Annual Tax Report " + report.year() + " ==="), false);
        source.sendSuccess(() -> Component.literal("Period: " + report.yearStart() + " - " + report.yearEnd()), false);
        source.sendSuccess(() -> Component.literal("Tax base: " + Money.format(report.taxableBase())), false);
        source.sendSuccess(() -> Component.literal("Assessed tax: " + Money.format(report.assessedTax())), false);
        source.sendSuccess(() -> Component.literal("Paid tax: " + Money.format(report.paidTax())), false);
        source.sendSuccess(() -> Component.literal("Refunds: " + Money.format(report.refunds()) + " (" + report.refundActions() + " action(s))"), false);
        source.sendSuccess(() -> Component.literal("Outstanding tax: " + Money.format(report.outstandingTax())), false);
        source.sendSuccess(() -> Component.literal("Current tax credit: " + Money.format(report.currentCreditBalance())), false);
        return 1;
    }
}
