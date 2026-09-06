package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.market.LogisticsSavedData;
import com.ailudick.capitalismmod.market.LogisticsCostSavedData;
import com.ailudick.capitalismmod.market.LogisticsLossSavedData;
import com.ailudick.capitalismmod.market.LogisticsClaimSavedData;
import com.ailudick.capitalismmod.supply.SupplyOrderAuditSavedData;
import com.ailudick.capitalismmod.supply.SupplyOrderAuditService;
import com.ailudick.capitalismmod.market.TradeRegion;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanySavedData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.ailudick.capitalismmod.market.LogisticsNodeSavedData;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;

/** Shows the player's current trade region and cargo still in transit. */
public final class LogisticsCommand {
    private LogisticsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("logistics")
                .executes(ctx -> list(ctx.getSource()))
                .then(Commands.literal("losses").executes(ctx -> losses(ctx.getSource())))
                .then(Commands.literal("claims").executes(ctx -> claims(ctx.getSource())))
                .then(Commands.literal("costs").executes(ctx -> costs(ctx.getSource())))
                .then(Commands.literal("repair")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> repair(ctx.getSource(), 0))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(0, 8))
                                .executes(ctx -> repair(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("order")
                        .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(ctx -> order(ctx.getSource(),
                                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("cancel")
                        .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(ctx -> cancel(ctx.getSource(),
                                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("insure")
                        .then(Commands.argument("shipment", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(ctx -> insure(ctx.getSource(),
                                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "shipment"))))));
    }

    private static int repair(CommandSourceStack source, int radius) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("A server world is required."));
            return 0;
        }
        ChunkPos chunk = new ChunkPos(player.blockPosition());
        int found = LogisticsNodeSavedData.get(source.getServer()).repair(level, chunk.x, chunk.z, radius);
        int scanned = (radius * 2 + 1) * (radius * 2 + 1);
        source.sendSuccess(() -> Component.literal("Logistics node repair scanned " + scanned
                + " loaded chunks and registered " + found + " existing facilities."), false);
        return found;
    }

    private static int losses(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        var records = LogisticsLossSavedData.get(source.getServer()).losses().stream()
                .filter(loss -> loss.buyer().equals(player.getUUID())).toList();
        source.sendSuccess(() -> Component.literal("=== Logistics loss records ==="), false);
        int start = Math.max(0, records.size() - 20);
        for (int i = start; i < records.size(); i++) {
            var loss = records.get(i);
            source.sendSuccess(() -> Component.literal(loss.itemId() + " x" + loss.quantity()
                    + " | " + loss.originRegion() + " -> " + loss.destinationRegion()
                    + " | disruptions " + loss.disruptionCount()
                    + (loss.supplyOrderId().isBlank() ? "" : " | order " + loss.supplyOrderId())), false);
        }
        if (records.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No lost cargo records."), false);
        }
        return records.size();
    }

    private static int claims(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        var records = LogisticsClaimSavedData.get(source.getServer()).claims().stream()
                .filter(claim -> claim.buyer().equals(player.getUUID())).toList();
        source.sendSuccess(() -> Component.literal("=== Logistics insurance claims ==="), false);
        int start = Math.max(0, records.size() - 20);
        for (int i = start; i < records.size(); i++) {
            var claim = records.get(i);
            source.sendSuccess(() -> Component.literal("shipment " + claim.shipmentId()
                    + " | loss " + claim.actualLoss() + " | insured " + claim.insuredValue()
                    + " | deductible " + claim.deductible() + " | payout " + claim.payout()
                    + (claim.carrierCompanyId().isBlank() ? "" : " | carrier " + claim.carrierCompanyId())
                    + " | " + claim.status()), false);
        }
        if (records.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No insurance claim records."), false);
        }
        return records.size();
    }

    private static int costs(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        var records = LogisticsCostSavedData.get(source.getServer()).forBuyer(player.getUUID());
        source.sendSuccess(() -> Component.literal("=== Logistics fuel cost plans ==="), false);
        int start = Math.max(0, records.size() - 20);
        for (int i = start; i < records.size(); i++) {
            var plan = records.get(i);
            source.sendSuccess(() -> Component.literal(plan.shipmentId().substring(0,
                            Math.min(8, plan.shipmentId().length())) + " | " + plan.itemId()
                    + " x" + plan.quantity() + " | " + plan.transport().id()
                    + " | fuel " + plan.fuelUnits() + "x " + plan.fuelItemId()
                    + " | estimated USD " + plan.estimatedCost()), false);
        }
        if (records.isEmpty()) source.sendSuccess(() -> Component.literal("No fuel cost plans."), false);
        return records.size();
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
            long distance = TradeRegion.distance(shipment.originRegion(), shipment.destinationRegion());
            int fuelUnits = shipment.transport().estimatedFuelUnits(shipment.quantity(), distance);
            source.sendSuccess(() -> Component.literal("Cargo " + shipment.itemId()
                    + " x" + shipment.quantity() + " | " + shipment.transport().id()
                    + " | " + shipment.originRegion() + " -> " + shipment.destinationRegion()
                    + " | ETA " + remaining + " ticks"
                    + " | planning fuel " + fuelUnits + "x " + shipment.transport().fuelItemId()), false);
            count++;
        }
        if (count == 0) {
            source.sendSuccess(() -> Component.literal("No cargo is currently in transit."), false);
        }
        return count;
    }

    private static int order(CommandSourceStack source, String orderId) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        var events = SupplyOrderAuditSavedData.get(source.getServer()).forOrder(orderId).stream()
                .filter(event -> event.buyerUuid().equals(player.getUUID())).toList();
        if (events.isEmpty()) {
            source.sendFailure(Component.literal("Order not found."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("=== Supply order " + orderId + " ==="), false);
        var activeOrder = com.ailudick.capitalismmod.supply.SupplyMarketSavedData.get(source.getServer())
                .orders().stream().filter(order -> order.id().equals(orderId)
                        && order.buyerUuid().equals(player.getUUID())).findFirst().orElse(null);
        if (activeOrder != null) {
            String quality = activeOrder.qualityScore() <= 0 ? "unverified"
                    : Integer.toString(activeOrder.qualityScore());
            source.sendSuccess(() -> Component.literal("Quality specification: " + quality), false);
        }
        String status = SupplyOrderAuditService.currentStatus(source.getServer(), orderId);
        source.sendSuccess(() -> Component.literal("Current status: " + status), false);
        for (var event : events) {
            source.sendSuccess(() -> Component.literal(event.type() + " | quantity " + event.quantity()
                    + " | amount " + event.amount() + " | tick " + event.occurredAt()), false);
        }
        return events.size();
    }

    private static int cancel(CommandSourceStack source, String orderId) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        boolean cancelled = com.ailudick.capitalismmod.supply.SupplyMarket.cancelOrder(player, orderId);
        if (!cancelled) {
            source.sendFailure(Component.literal("Order not found, already delivered, or not cancellable."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Undelivered supply order cancelled; remaining escrow refunded."), false);
        return 1;
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
        long premium;
        try {
            premium = BigDecimal.valueOf(declared)
                    .multiply(BigDecimal.valueOf(Config.LOGISTICS_INSURANCE_RATE.get()))
                    .setScale(0, RoundingMode.CEILING)
                    .max(BigDecimal.ONE)
                    .longValueExact();
        } catch (ArithmeticException e) {
            source.sendFailure(Component.literal("Insurance premium is too large."));
            return 0;
        }
        long premiumMinor = Money.toMinor(premium);
        if (premiumMinor < 0) {
            source.sendFailure(Component.literal("Insufficient USD for insurance."));
            return 0;
        }
        Company company = shipment.buyerCompanyId().isBlank()
                ? null : CompanySavedData.get(source.getServer()).get(shipment.buyerCompanyId());
        boolean companyShipment = company != null && company.ownerUuid().equals(player.getUUID());
        boolean paid = companyShipment
                ? CompanyHelper.debitTreasuryNonOperatingOnce(source.getServer(), company.companyId(),
                        Currencies.USD.id(), premium, "cargo_insurance", "Cargo insurance premium",
                        "cargo-insurance:" + id)
                : EconomyHelper.tryPayWithReference(player, Currencies.USD, premiumMinor,
                        "cargo-insurance:" + id);
        if (!paid) {
            source.sendFailure(Component.literal(companyShipment
                    ? "Insufficient company USD for insurance." : "Insufficient USD for insurance."));
            return 0;
        }
        if (!data.insure(id, player.getUUID())) {
            if (companyShipment) {
                CompanyHelper.creditTreasuryNonOperatingOnce(source.getServer(), company.companyId(),
                        Currencies.USD.id(), premium, "cargo_insurance_refund",
                        "Failed insurance enrollment refund", "cargo-insurance-refund:" + id);
            } else {
                MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(source.getServer());
                String refundSource = "cargo-insurance-refund:" + id;
                if (!mailbox.hasCreditSource(refundSource)) {
                    mailbox.creditMoneyOnce(player.getUUID(), Currencies.USD.id(), premiumMinor, refundSource);
                }
                mailbox.redeem(player);
            }
            source.sendFailure(Component.literal("Shipment could not be insured; payment was refunded."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Shipment insured for USD " + declared
                + " (premium USD " + premium + ")."), false);
        return 1;
    }
}
