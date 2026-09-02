package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.economy.MarketTradeSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Displays persisted market fills for players and administrators. */
public final class MarketTradesCommand {
    private MarketTradesCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("markettrades")
                .executes(ctx -> list(ctx.getSource(), 10, false))
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                        .executes(ctx -> list(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"), false)))
                .then(Commands.literal("all")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> list(ctx.getSource(), 20, true))));
    }

    private static int list(CommandSourceStack source, int limit, boolean all) {
        ServerPlayer player = source.getEntity() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        var trades = MarketTradeSavedData.get(source.getServer()).trades();
        int shown = 0;
        for (int i = trades.size() - 1; i >= 0 && shown < limit; i--) {
            MarketTradeSavedData.Trade trade = trades.get(i);
            if (!all && player != null && !trade.involves(player.getUUID())) {
                continue;
            }
            source.sendSuccess(() -> Component.literal(trade.market() + " " + trade.itemId()
                    + " 数量=" + trade.quantity() + " 总额=" + trade.total() + " 手续费=" + trade.fee()), false);
            shown++;
        }
        if (shown == 0) {
            source.sendSuccess(() -> Component.literal("暂无成交记录。"), false);
        }
        return shown;
    }
}
