package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.economy.EconomyLogSavedData;
import com.ailudick.capitalismmod.currency.Money;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Displays the player's recent successful wallet settlements. */
public final class EconomyLogCommand {
    private EconomyLogCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("economylog").executes(ctx -> list(ctx.getSource().getPlayerOrException())));
    }

    private static int list(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("最近经济流水："));
        int shown = 0;
        var entries = EconomyLogSavedData.get(player.getServer()).entries();
        for (int i = entries.size() - 1; i >= 0 && shown < 10; i--) {
            EconomyLogSavedData.Entry entry = entries.get(i);
            if (!entry.playerId().equals(player.getUUID())) {
                continue;
            }
            player.sendSystemMessage(Component.literal(entry.action() + " "
                    + Money.format(entry.amount()) + " " + entry.currencyId()));
            shown++;
        }
        if (shown == 0) {
            player.sendSystemMessage(Component.literal("暂无流水记录。"));
        }
        return 1;
    }
}
