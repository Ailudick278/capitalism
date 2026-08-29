package com.ailudick.capitalismmod.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * Broadcasts economy news to the whole server. Called from price-update tick handlers
 * and other system events when something newsworthy happens.
 */
public final class EconomyNews {
    private EconomyNews() {
    }

    /** Sends a translated system message to all players. */
    public static void broadcast(MinecraftServer server, String key, Object... args) {
        server.getPlayerList().broadcastSystemMessage(Component.translatable(key, args), false);
    }
}
