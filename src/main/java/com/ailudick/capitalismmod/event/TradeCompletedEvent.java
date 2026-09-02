package com.ailudick.capitalismmod.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/**
 * Fired when a trade completes (shop purchase, commodity exchange fill, or stock trade).
 */
public class TradeCompletedEvent extends Event {
    private final Player buyer;
    private final Player seller;
    private final ItemStack item;
    private final int quantity;
    private final String currencyId;
    private final long total;
    private final String market;
    private final long fee;
    private final String assetId;

    public TradeCompletedEvent(Player buyer, Player seller, ItemStack item, int quantity, String currencyId, long total) {
        this(buyer, seller, item, quantity, currencyId, total, "unknown", 0L, null);
    }

    public TradeCompletedEvent(Player buyer, Player seller, ItemStack item, int quantity, String currencyId,
                               long total, String market, long fee) {
        this(buyer, seller, item, quantity, currencyId, total, market, fee, null);
    }

    public TradeCompletedEvent(Player buyer, Player seller, ItemStack item, int quantity, String currencyId,
                               long total, String market, long fee, String assetId) {
        this.buyer = buyer;
        this.seller = seller;
        this.item = item;
        this.quantity = quantity;
        this.currencyId = currencyId;
        this.total = total;
        this.market = market;
        this.fee = fee;
        this.assetId = assetId;
    }

    public Player getBuyer() {
        return buyer;
    }

    public Player getSeller() {
        return seller;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public long getTotal() {
        return total;
    }

    public String getMarket() {
        return market;
    }

    public long getFee() {
        return fee;
    }

    public String getAssetId() {
        return assetId;
    }
}
