package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class BusinessLicenseMenu extends AbstractContainerMenu {
    public BusinessLicenseMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.BUSINESS_LICENSE_MENU.get(), containerId);
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
