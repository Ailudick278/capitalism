package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.init.ModMenuTypes;
import com.ailudick.capitalismmod.supply.PurchaseOrder;
import com.ailudick.capitalismmod.supply.SupplyOffer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ProcurementMenu extends AbstractContainerMenu {
    private List<SupplyOffer> offers = new ArrayList<>();
    private List<PurchaseOrder> orders = new ArrayList<>();

    public ProcurementMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.PROCUREMENT_MENU.get(), containerId);
    }

    public List<SupplyOffer> getOffers() {
        return offers;
    }

    public void setOffers(List<SupplyOffer> offers) {
        this.offers = offers;
    }

    public List<PurchaseOrder> getOrders() {
        return orders;
    }

    public void setOrders(List<PurchaseOrder> orders) {
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
