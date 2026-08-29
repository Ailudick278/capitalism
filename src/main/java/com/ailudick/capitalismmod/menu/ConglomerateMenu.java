package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.company.Conglomerate;
import com.ailudick.capitalismmod.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class ConglomerateMenu extends AbstractContainerMenu {
    private Conglomerate conglomerate = Conglomerate.create("");

    public ConglomerateMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.CONGLOMERATE_MENU.get(), containerId);
    }

    public Conglomerate getConglomerate() {
        return conglomerate;
    }

    public void setConglomerate(Conglomerate conglomerate) {
        this.conglomerate = conglomerate;
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
