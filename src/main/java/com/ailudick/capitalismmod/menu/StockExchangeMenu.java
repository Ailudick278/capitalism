package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.init.ModMenuTypes;
import com.ailudick.capitalismmod.stock.Candle;
import com.ailudick.capitalismmod.stock.StockOrder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockExchangeMenu extends AbstractContainerMenu {
    private Map<String, Long> prices = new HashMap<>();
    private Map<String, Long> portfolio = new HashMap<>();
    private Map<String, List<Candle>> history = new HashMap<>();
    private Map<String, String> companies = new HashMap<>();
    private List<StockOrder> orders = new ArrayList<>();

    public StockExchangeMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.STOCK_EXCHANGE_MENU.get(), containerId);
    }


    public Map<String, Long> getPrices() {
        return prices;
    }

    public void setPrices(Map<String, Long> prices) {
        this.prices = prices;
    }

    public Map<String, Long> getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Map<String, Long> portfolio) {
        this.portfolio = portfolio;
    }

    public Map<String, List<Candle>> getHistory() {
        return history;
    }

    public void setHistory(Map<String, List<Candle>> history) {
        this.history = history;
    }

    public Map<String, String> getCompanies() {
        return companies;
    }

    public void setCompanies(Map<String, String> companies) {
        this.companies = companies;
    }

    public List<StockOrder> getOrders() {
        return orders;
    }

    public void setOrders(List<StockOrder> orders) {
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
