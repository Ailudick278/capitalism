package com.ailudick.capitalismmod.market;

import com.ailudick.capitalismmod.company.CompanyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Records cargo losses and delivers the corresponding player notification. */
public final class LogisticsLossService {
    private LogisticsLossService() {
    }

    public static void record(MinecraftServer server, LogisticsSavedData.Shipment shipment) {
        LogisticsLossSavedData data = LogisticsLossSavedData.get(server);
        data.add(new LogisticsLossSavedData.Loss(shipment.id(), shipment.buyer(), shipment.itemId(),
                shipment.quantity(), shipment.originRegion(), shipment.destinationRegion(), shipment.transport(),
                shipment.disruptionCount() + 1, server.overworld().getGameTime(), false, shipment.supplyOrderId(),
                shipment.buyerCompanyId(), shipment.unitPrice()));
        if (!shipment.buyerCompanyId().isBlank() && shipment.unitPrice() > 0L) {
            long loss;
            try {
                loss = Math.multiplyExact((long) shipment.quantity(), shipment.unitPrice());
            } catch (ArithmeticException e) {
                loss = Long.MAX_VALUE;
            }
            CompanyHelper.recordInventoryLoss(server, shipment.buyerCompanyId(), loss, shipment.id());
        }
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
