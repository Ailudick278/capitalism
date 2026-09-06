package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.economy.expansion.EconomicEventService;
import com.ailudick.capitalismmod.economy.expansion.EconomicExpansionSavedData;
import com.ailudick.capitalismmod.market.Commodities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Comparator;

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
        var logisticsDays = Commands.argument("days", IntegerArgumentType.integer(1, 365))
                .executes(c -> logistics(c.getSource(), StringArgumentType.getString(c, "eventId"),
                        StringArgumentType.getString(c, "origin"), StringArgumentType.getString(c, "destination"),
                        IntegerArgumentType.getInteger(c, "capacityBps"), IntegerArgumentType.getInteger(c, "days")));
        var capacity = Commands.argument("capacityBps", IntegerArgumentType.integer(-9000, 9000)).then(logisticsDays);
        var destination = Commands.argument("destination", StringArgumentType.word()).then(capacity);
        var origin = Commands.argument("origin", StringArgumentType.word()).then(destination);
        var logisticsShock = Commands.literal("logisticsShock").then(Commands.argument("eventId", StringArgumentType.word()).then(origin));
        var list = Commands.literal("list").executes(c -> list(c.getSource()));
        dispatcher.register(Commands.literal("economicevent").requires(s -> s.hasPermission(2))
                .then(priceShock).then(logisticsShock).then(list));
    }

    private static int logistics(CommandSourceStack source, String eventId, String origin, String destination,
                                 int capacityBps, int days) {
        long now = source.getServer().overworld().getGameTime();
        if (!EconomicEventService.addLogisticsCapacityShock(source.getServer(), eventId, origin, destination,
                capacityBps, now, days)) return 0;
        source.sendSuccess(() -> Component.literal("Logistics capacity shock created: " + eventId), true);
        return 1;
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

    private static int list(CommandSourceStack source) {
        long now = source.getServer().overworld().getGameTime();
        var events = EconomicExpansionSavedData.get(source.getServer()).events().stream()
                .filter(event -> event.activeAt(now))
                .sorted(Comparator.comparingLong(event -> event.endsAt() == 0L ? Long.MAX_VALUE : event.endsAt()))
                .toList();
        source.sendSuccess(() -> Component.literal("Active economic events: " + events.size()), false);
        events.stream().limit(20).forEach(event -> source.sendSuccess(() -> Component.literal(
                event.id() + " | " + event.type() + " | target="
                        + (event.target() == null ? "-" : event.target().id())
                        + " | remainingTicks=" + Math.max(0L, event.endsAt() - now)), false));
        return events.isEmpty() ? 0 : 1;
    }
}
