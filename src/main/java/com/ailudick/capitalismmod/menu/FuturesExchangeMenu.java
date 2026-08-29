package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.futures.Position;
import com.ailudick.capitalismmod.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FuturesExchangeMenu extends AbstractContainerMenu {
    private Map<String, Long> prices = new HashMap<>();
    private List<Position> positions = new ArrayList<>();
    private long marginBalance = 0L;
    private Map<String, Long> daysToExpiry = new HashMap<>();

    public FuturesExchangeMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.FUTURES_EXCHANGE_MENU.get(), containerId);
    }

    public Map<String, Long> getPrices() {
        return prices;
    }

    public void setPrices(Map<String, Long> prices) {
        this.prices = prices;
    }

    public List<Position> getPositions() {
        return positions;
    }

    public void setPositions(List<Position> positions) {
        this.positions = positions;
    }

    public long getMarginBalance() {
        return marginBalance;
    }

    public void setMarginBalance(long marginBalance) {
        this.marginBalance = marginBalance;
    }

    public Map<String, Long> getDaysToExpiry() {
        return daysToExpiry;
    }

    public void setDaysToExpiry(Map<String, Long> daysToExpiry) {
        this.daysToExpiry = daysToExpiry;
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
