package com.ailudick.capitalismmod.tax;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public final class TaxRefundNotificationService {
    private TaxRefundNotificationService() {}
    public static void notify(MinecraftServer server, UUID playerUuid, String message) {
        notify(server, "", playerUuid, message);
    }
    public static void notify(MinecraftServer server, String requestId, UUID playerUuid, String message) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player != null) player.displayClientMessage(Component.literal(message), true);
        else TaxRefundNotificationSavedData.get(server).add(new TaxRefundNotificationSavedData.Notification(playerUuid, requestId, server.overworld().getGameTime(), message));
    }
    public static void deliver(ServerPlayer player) {
        var data = TaxRefundNotificationSavedData.get(player.getServer());
        for (var notification : data.unreadFor(player.getUUID())) player.displayClientMessage(Component.literal(notification.message()), false);
        data.markRead(player.getUUID());
    }
}
