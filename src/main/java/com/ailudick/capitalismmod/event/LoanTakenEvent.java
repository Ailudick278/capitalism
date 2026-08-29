package com.ailudick.capitalismmod.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

/**
 * Fired when a player takes a loan on a credit account.
 */
public class LoanTakenEvent extends Event {
    private final Player player;
    private final String accountId;
    private final String currencyId;
    private final long amount;

    public LoanTakenEvent(Player player, String accountId, String currencyId, long amount) {
        this.player = player;
        this.accountId = accountId;
        this.currencyId = currencyId;
        this.amount = amount;
    }

    public Player getPlayer() {
        return player;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public long getAmount() {
        return amount;
    }
}
