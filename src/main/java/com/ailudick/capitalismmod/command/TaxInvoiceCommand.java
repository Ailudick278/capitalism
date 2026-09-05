package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.tax.TaxInvoiceSavedData;
import com.ailudick.capitalismmod.currency.Money;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Player-facing VAT input/output invoice history. */
public final class TaxInvoiceCommand {
    private TaxInvoiceCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("taxinvoices")
                .executes(context -> list(context.getSource())));
    }

    private static int list(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var invoices = TaxInvoiceSavedData.get(player.getServer()).forTaxpayer(player.getUUID());
        int start = Math.max(0, invoices.size() - 20);
        for (int i = start; i < invoices.size(); i++) {
            TaxInvoiceSavedData.Invoice invoice = invoices.get(i);
            source.sendSuccess(() -> Component.literal(invoice.direction() + " VAT | gross "
                    + Money.format(invoice.grossAmount()) + " | tax " + Money.format(invoice.taxAmount())
                    + " | credit " + Money.format(invoice.creditApplied())
                    + " | source " + invoice.sourceEventId()), false);
        }
        if (invoices.isEmpty()) source.sendSuccess(() -> Component.literal("No VAT invoice records."), false);
        return invoices.size();
    }
}
