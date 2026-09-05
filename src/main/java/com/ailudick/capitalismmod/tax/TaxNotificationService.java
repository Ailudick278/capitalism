package com.ailudick.capitalismmod.tax;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/** Delivers tax notices immediately when possible and persists them otherwise. */
public final class TaxNotificationService {
    private TaxNotificationService() {}

    public static void notify(MinecraftServer server, UUID playerUuid, String noticeId, String message) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            player.displayClientMessage(Component.literal(message), true);
            return;
        }
        TaxNotificationSavedData.get(server).add(new TaxNotificationSavedData.Notification(
                playerUuid, noticeId, server.overworld().getGameTime(), message, false));
    }

    public static void deliver(ServerPlayer player) {
        TaxNotificationSavedData data = TaxNotificationSavedData.get(player.getServer());
        for (var notification : data.unreadFor(player.getUUID())) {
            player.displayClientMessage(Component.literal("税务通知：" + notification.message()), false);
        }
        data.markRead(player.getUUID());
    }
}
