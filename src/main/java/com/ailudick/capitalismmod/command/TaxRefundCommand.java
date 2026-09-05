package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.tax.TaxRefundSavedData;
import com.ailudick.capitalismmod.tax.TaxRefundService;
import com.ailudick.capitalismmod.tax.TaxRefundAuditSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import com.ailudick.capitalismmod.menu.TaxBureauMenu;
import com.ailudick.capitalismmod.network.ServerPayloadHandler;

/** Admin review commands for player tax refund requests. */
public final class TaxRefundCommand {
    private TaxRefundCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("taxrefund")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("ui").executes(context -> openUi(context.getSource())))
                .then(Commands.literal("history").executes(context -> history(context.getSource())))
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
                .then(Commands.literal("approve")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(context -> approve(context.getSource(), StringArgumentType.getString(context, "id")))))
                .then(Commands.literal("reject")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(context -> reject(context.getSource(),
                                                StringArgumentType.getString(context, "id"),
                                                StringArgumentType.getString(context, "reason")))))));
    }

    private static int openUi(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.openMenu(new SimpleMenuProvider((id, inv, ignored) -> new TaxBureauMenu(id, inv), Component.literal("Tax Refund Review")));
        ServerPayloadHandler.sendTaxBills(player);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        var requests = TaxRefundSavedData.get(source.getServer()).all();
        if (requests.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No tax refund requests."), false);
            return 0;
        }
        for (TaxRefundSavedData.Request request : requests) {
            source.sendSuccess(() -> Component.literal(request.id() + " | " + request.status() + " | "
                    + request.taxpayerUuid() + " | " + request.currencyId().toUpperCase() + " "
                    + Money.format(request.amount())), false);
        }
        return requests.size();
    }

    private static int history(CommandSourceStack source) {
        var events = TaxRefundAuditSavedData.get(source.getServer()).all();
        if (events.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No tax refund audit events."), false);
            return 0;
        }
        for (var event : events) {
            source.sendSuccess(() -> Component.literal(event.time() + " | " + event.action() + " | "
                    + event.requestId() + " | " + event.actor() + " | " + event.result() + " | " + event.reason()), false);
        }
        return events.size();
    }

    private static int approve(CommandSourceStack source, String id) {
        boolean approved = TaxRefundService.approve(source.getServer(), id, source.getTextName());
        source.sendSuccess(() -> Component.literal(approved ? "Tax refund approved." : "Tax refund approval failed."), true);
        return approved ? 1 : 0;
    }

    private static int reject(CommandSourceStack source, String id, String reason) {
        boolean rejected = TaxRefundService.reject(source.getServer(), id, source.getTextName(), reason);
        source.sendSuccess(() -> Component.literal(rejected ? "Tax refund rejected." : "Tax refund rejection failed."), true);
        return rejected ? 1 : 0;
    }
}
