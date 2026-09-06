package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.economy.expansion.EconomicEventService;
import com.ailudick.capitalismmod.market.Commodities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Operator command for deterministic, time-bounded commodity shocks. */
public final class EconomicEventCommand {
    private EconomicEventCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var days = Commands.argument("days", IntegerArgumentType.integer(1, 365))
                .executes(c -> create(c.getSource(), StringArgumentType.getString(c, "eventId"),
                        StringArgumentType.getString(c, "itemId"),
                        IntegerArgumentType.getInteger(c, "shockBps"),
                        IntegerArgumentType.getInteger(c, "days")));
        var shock = Commands.argument("shockBps", IntegerArgumentType.integer(-9000, 9000)).then(days);
        var item = Commands.argument("itemId", StringArgumentType.word()).then(shock);
        var id = Commands.argument("eventId", StringArgumentType.word()).then(item);
        var priceShock = Commands.literal("priceShock").then(id);
        dispatcher.register(Commands.literal("economicevent").requires(s -> s.hasPermission(2)).then(priceShock));
    }

    private static int create(CommandSourceStack source, String eventId, String itemId, int shockBps, int days) {
        if (Commodities.initialPrice(itemId) <= 0L) {
            source.sendFailure(Component.literal("Unknown commodity: " + itemId));
            return 0;
        }
        long now = source.getServer().overworld().getGameTime();
        if (!EconomicEventService.addCommodityPriceShock(source.getServer(), eventId, itemId, shockBps, now, days)) return 0;
        source.sendSuccess(() -> Component.literal("Economic price shock created: " + eventId), true);
        return 1;
    }
}
