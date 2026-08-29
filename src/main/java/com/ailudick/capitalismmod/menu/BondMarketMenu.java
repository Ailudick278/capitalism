package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.bond.BondHolding;
import com.ailudick.capitalismmod.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BondMarketMenu extends AbstractContainerMenu {
    private List<BondHolding> holdings = new ArrayList<>();

    public BondMarketMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.BOND_MARKET_MENU.get(), containerId);
    }

    public List<BondHolding> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<BondHolding> holdings) {
        this.holdings = holdings;
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
