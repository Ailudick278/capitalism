package com.ailudick.capitalismmod.market;

import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanyInventoryCostSavedData;
import com.ailudick.capitalismmod.supply.SupplyOrderAuditService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Records cargo losses and delivers the corresponding player notification. */
public final class LogisticsLossService {
    private LogisticsLossService() {
    }

    public static void record(MinecraftServer server, LogisticsSavedData.Shipment shipment) {
        LogisticsLossSavedData data = LogisticsLossSavedData.get(server);
        // A retry can happen after the shipment was processed but before it was removed.
        // The loss ledger is the durable idempotency key for the loss path.
        if (data.hasShipment(shipment.id())) {
            return;
        }
        if (!shipment.buyerCompanyId().isBlank() && shipment.unitPrice() > 0L) {
            long loss;
            try {
                loss = Math.multiplyExact((long) shipment.quantity(), shipment.unitPrice());
            } catch (ArithmeticException e) {
                loss = Long.MAX_VALUE;
            }
            CompanyInventoryCostSavedData.Consumption tracked = CompanyInventoryCostSavedData.get(server)
                    .consume(shipment.buyerCompanyId(), shipment.itemId(), shipment.quantity());
            if (tracked.quantity() > 0) {
                long fallback;
                try {
                    fallback = Math.multiplyExact((long) shipment.quantity() - tracked.quantity(), shipment.unitPrice());
                } catch (ArithmeticException e) {
                    fallback = Long.MAX_VALUE;
                }
                loss = tracked.cost() >= Long.MAX_VALUE - fallback ? Long.MAX_VALUE : tracked.cost() + fallback;
            }
            CompanyHelper.recordInventoryLoss(server, shipment.buyerCompanyId(), loss, shipment.id());
        }
        if (!shipment.supplyOrderId().isBlank() && shipment.supplierUuid() != null) {
            long amount;
            try {
                amount = Math.multiplyExact((long) shipment.quantity(), shipment.unitPrice());
            } catch (ArithmeticException e) {
                amount = Long.MAX_VALUE;
            }
            SupplyOrderAuditService.record(server, shipment.supplyOrderId(), "LOST", shipment.buyer(),
                    shipment.supplierUuid(), shipment.itemId(), shipment.quantity(), amount, shipment.id());
        }
        data.add(new LogisticsLossSavedData.Loss(shipment.id(), shipment.buyer(), shipment.itemId(),
                shipment.quantity(), shipment.originRegion(), shipment.destinationRegion(), shipment.transport(),
                shipment.disruptionCount() + 1, server.overworld().getGameTime(), false, shipment.supplyOrderId(),
                shipment.buyerCompanyId(), shipment.unitPrice()));
        ServerPlayer player = server.getPlayerList().getPlayer(shipment.buyer());
        if (player != null) {
            player.displayClientMessage(Component.literal("物流通知：货物 " + shipment.itemId() + " x"
                    + shipment.quantity() + " 已因连续运输中断而损失。可使用 /logistics losses 查看记录。"), true);
            data.acknowledge(shipment.buyer());
        }
    }

    public static void deliver(ServerPlayer player) {
        LogisticsLossSavedData data = LogisticsLossSavedData.get(player.getServer());
        for (LogisticsLossSavedData.Loss loss : data.unreadFor(player.getUUID())) {
            player.displayClientMessage(Component.literal("物流通知：货物 " + loss.itemId() + " x"
                    + loss.quantity() + " 在运输中损失（中断 " + loss.disruptionCount() + " 次）。"), false);
        }
        data.acknowledge(player.getUUID());
    }
}
