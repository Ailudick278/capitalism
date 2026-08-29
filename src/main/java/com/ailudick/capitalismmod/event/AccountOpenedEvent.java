package com.ailudick.capitalismmod.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

/**
 * Fired when a player opens a bank account.
 */
public class AccountOpenedEvent extends Event {
    private final Player player;
    private final String accountId;
    private final boolean credit;

    public AccountOpenedEvent(Player player, String accountId, boolean credit) {
        this.player = player;
        this.accountId = accountId;
        this.credit = credit;
    }

    public Player getPlayer() {
        return player;
    }

    public String getAccountId() {
        return accountId;
    }

    public boolean isCredit() {
        return credit;
    }
}
