package com.ailudick.capitalismmod.loan;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Delivers peer-loan due and delinquency notices immediately or after login. */
public final class PeerLoanNotificationService {
    private PeerLoanNotificationService() {}

    public static void notify(MinecraftServer server, UUID playerUuid, String noticeId, String message) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            player.displayClientMessage(Component.literal(message), true);
            return;
        }
        PeerLoanNotificationSavedData.get(server).add(new PeerLoanNotificationSavedData.Notification(
                playerUuid, noticeId, server.overworld().getGameTime(), message, false));
    }

    public static void deliver(ServerPlayer player) {
        PeerLoanNotificationSavedData data = PeerLoanNotificationSavedData.get(player.getServer());
        for (var notification : data.unreadFor(player.getUUID())) {
            player.displayClientMessage(Component.literal("贷款通知：" + notification.message()), false);
        }
        data.markRead(player.getUUID());
    }
}
