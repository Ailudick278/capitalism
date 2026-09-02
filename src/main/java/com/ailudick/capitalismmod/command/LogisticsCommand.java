package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.market.LogisticsSavedData;
import com.ailudick.capitalismmod.market.TradeRegion;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Shows the player's current trade region and cargo still in transit. */
public final class LogisticsCommand {
    private LogisticsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("logistics")
                .executes(ctx -> list(ctx.getSource())));
    }

    private static int list(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        MinecraftServer server = source.getServer();
        String region = TradeRegion.of(player.blockPosition());
        long now = server.overworld().getGameTime();
        source.sendSuccess(() -> Component.literal("Current trade region: " + region), false);

        int count = 0;
        for (LogisticsSavedData.Shipment shipment : LogisticsSavedData.get(server).shipments()) {
            if (!shipment.buyer().equals(player.getUUID())) {
                continue;
            }
            long remaining = Math.max(0L, shipment.deliveryTick() - now);
            source.sendSuccess(() -> Component.literal("Cargo " + shipment.itemId()
                    + " x" + shipment.quantity() + " | " + shipment.transport().id()
                    + " | " + shipment.originRegion() + " -> " + shipment.destinationRegion()
                    + " | ETA " + remaining + " ticks"), false);
            count++;
        }
        if (count == 0) {
            source.sendSuccess(() -> Component.literal("No cargo is currently in transit."), false);
        }
        return count;
    }
}
