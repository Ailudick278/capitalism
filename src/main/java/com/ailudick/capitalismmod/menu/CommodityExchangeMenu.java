package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.init.ModMenuTypes;
import com.ailudick.capitalismmod.market.MarketOrder;
import com.ailudick.capitalismmod.stock.Candle;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommodityExchangeMenu extends AbstractContainerMenu {
    private Map<String, Long> prices = new HashMap<>();
    private Map<String, List<Candle>> history = new HashMap<>();
    private List<MarketOrder> orders = new ArrayList<>();

    public CommodityExchangeMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.COMMODITY_EXCHANGE_MENU.get(), containerId);
    }

    public Map<String, Long> getPrices() {
        return prices;
    }

    public void setPrices(Map<String, Long> prices) {
        this.prices = prices;
    }

    public Map<String, List<Candle>> getHistory() {
        return history;
    }

    public void setHistory(Map<String, List<Candle>> history) {
        this.history = history;
    }

    public List<MarketOrder> getOrders() {
        return orders;
    }

    public void setOrders(List<MarketOrder> orders) {
        this.orders = orders;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
