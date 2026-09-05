package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.tax.TaxBill;
import com.ailudick.capitalismmod.tax.TaxLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Player-facing transaction-tax history and receipt commands. */
public final class TaxTransactionCommand {
    private TaxTransactionCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("taxtransactions")
                .executes(context -> list(context.getSource())));
        dispatcher.register(Commands.literal("taxreceipt")
                .then(Commands.argument("billId", StringArgumentType.word())
                        .executes(context -> receipt(context.getSource(),
                                StringArgumentType.getString(context, "billId")))));
    }

    private static int list(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var bills = TaxLedgerSavedData.get(player.getServer()).bills().stream()
                .filter(bill -> isTransactionTax(bill.subject().type())
                        && (player.hasPermissions(2) || bill.subject().taxpayerUuid().equals(player.getUUID())))
                .toList();
        if (bills.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No transaction tax records."), false);
            return 0;
        }
        for (TaxBill bill : bills) {
            source.sendSuccess(() -> Component.literal(bill.id() + " | source=" + bill.sourceEventId()
                    + " | base=" + Money.format(bill.taxableBase()) + " | tax=" + Money.format(bill.amount())
                    + " | paid=" + Money.format(bill.paidAmount())), false);
        }
        return bills.size();
    }

    private static int receipt(CommandSourceStack source, String id)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TaxBill bill = TaxLedgerSavedData.get(player.getServer()).get(id);
        if (bill == null || !isTransactionTax(bill.subject().type())
                || (!player.hasPermissions(2) && !bill.subject().taxpayerUuid().equals(player.getUUID()))) {
            source.sendFailure(Component.literal("Transaction tax receipt not found."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("=== Transaction Tax Receipt ==="), false);
        source.sendSuccess(() -> Component.literal("Receipt: " + bill.id()), false);
        source.sendSuccess(() -> Component.literal("Source: " + bill.sourceEventId()), false);
        source.sendSuccess(() -> Component.literal("Taxpayer: " + bill.subject().taxpayerUuid()), false);
        source.sendSuccess(() -> Component.literal("Gross amount: " + Money.format(bill.taxableBase())), false);
        source.sendSuccess(() -> Component.literal("Rate: " + (bill.rateBasisPoints() / 100.0) + "%"), false);
        source.sendSuccess(() -> Component.literal("Tax: " + Money.format(bill.amount())), false);
        source.sendSuccess(() -> Component.literal("Paid: " + Money.format(bill.paidAmount())), false);
        return 1;
    }

    private static boolean isTransactionTax(TaxType type) {
        return type == TaxType.TRANSACTION || type == TaxType.VAT || type == TaxType.LAND_TRANSFER
                || type == TaxType.STAMP_DUTY || type == TaxType.CAPITAL_GAINS;
    }
}
