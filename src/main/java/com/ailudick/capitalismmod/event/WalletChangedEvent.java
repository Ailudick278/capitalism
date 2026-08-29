package com.ailudick.capitalismmod.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

/**
 * Fired when a player's wallet balance changes.
 */
public class WalletChangedEvent extends Event {
    private final Player player;
    private final String currencyId;
    private final long newBalance;

    public WalletChangedEvent(Player player, String currencyId, long newBalance) {
        this.player = player;
        this.currencyId = currencyId;
        this.newBalance = newBalance;
    }

    public Player getPlayer() {
        return player;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public long getNewBalance() {
        return newBalance;
    }
}
