package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.economy.EconomySavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * /shares give — over-the-counter transfer of the sender's shares to another player.
 */
public class TransferSharesCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shares")
                .then(Commands.literal("give")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("stock", StringArgumentType.word())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                .executes(ctx -> {
                                                    ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                    String stock = StringArgumentType.getString(ctx, "stock");
                                                    long amount = LongArgumentType.getLong(ctx, "amount");
                                                    return transfer(sender, target, stock, amount);
                                                }))))));
    }

    private static int transfer(ServerPlayer sender, ServerPlayer target, String stock, long amount) {
        EconomySavedData data = EconomySavedData.get(sender.getServer());
        String stockId = resolveStockId(data, stock);
        if (stockId == null) {
            sender.sendSystemMessage(Component.translatable("command.capitalismmod.stock_not_found", stock));
            return 0;
        }
        if (data.holdings(stockId, sender.getUUID()) < amount) {
            sender.sendSystemMessage(Component.translatable("command.capitalismmod.insufficient"));
            return 0;
        }
        data.addShares(stockId, sender.getUUID(), -amount);
        data.addShares(stockId, target.getUUID(), amount);
        sender.sendSystemMessage(Component.translatable("command.capitalismmod.shares_transferred",
                amount, stockId, target.getDisplayName()));
        return 1;
    }

    /** Resolves an abstract stock id directly, or a listed company name to its stock id. */
    private static String resolveStockId(EconomySavedData data, String input) {
        if (data.isStock(input)) {
            return input;
        }
        for (Map.Entry<String, EconomySavedData.Listing> entry : data.listings().entrySet()) {
            if (entry.getValue().name().equalsIgnoreCase(input)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
