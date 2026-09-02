package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.market.LogisticsSavedData;
import com.ailudick.capitalismmod.market.TradeRegion;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.wallet.EconomyHelper;

/** Shows the player's current trade region and cargo still in transit. */
public final class LogisticsCommand {
    private LogisticsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("logistics")
                .executes(ctx -> list(ctx.getSource()))
                .then(Commands.literal("insure")
                        .then(Commands.argument("shipment", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(ctx -> insure(ctx.getSource(),
                                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "shipment"))))));
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

    private static int insure(CommandSourceStack source, String id) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        LogisticsSavedData data = LogisticsSavedData.get(source.getServer());
        LogisticsSavedData.Shipment shipment = data.shipments().stream()
                .filter(candidate -> candidate.id().equals(id) && candidate.buyer().equals(player.getUUID()))
                .findFirst().orElse(null);
        if (shipment == null || shipment.insured()) {
            source.sendFailure(Component.literal("Shipment not found or already insured."));
            return 0;
        }
        long declared;
        try {
            declared = Math.multiplyExact((long) shipment.quantity(), Config.LOGISTICS_DECLARED_VALUE.get());
        } catch (ArithmeticException e) {
            source.sendFailure(Component.literal("Shipment value is too large."));
            return 0;
        }
        long premium = Math.max(1L, (long) (declared * Config.LOGISTICS_INSURANCE_RATE.get()));
        if (!EconomyHelper.tryPay(player, Currencies.USD, Money.toMinor(premium))
                || !data.insure(id, player.getUUID())) {
            source.sendFailure(Component.literal("Insufficient USD for insurance."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Shipment insured for USD " + declared
                + " (premium USD " + premium + ")."), false);
        return 1;
    }
}
