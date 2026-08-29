package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class WarehouseMenu extends AbstractContainerMenu {
    private Map<String, Integer> storage = new HashMap<>();

    public WarehouseMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.WAREHOUSE_MENU.get(), containerId);
    }

    public Map<String, Integer> getStorage() {
        return storage;
    }

    public void setStorage(Map<String, Integer> storage) {
        this.storage = storage;
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
