package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.blockentity.ShopBlockEntity;
import com.ailudick.capitalismmod.init.ModMenuTypes;
import com.ailudick.capitalismmod.shop.ShopOffer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopMenu extends AbstractContainerMenu {
    private final List<ShopOffer> offers = new ArrayList<>();

    // Client constructor: used by MenuType's MenuSupplier (int, Inventory). Data arrives via payloads.
    public ShopMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.SHOP_MENU.get(), containerId);
    }

    // Server constructor: directly references the shop block entity.
    public ShopMenu(int containerId, Inventory playerInventory, ShopBlockEntity shop) {
        super(ModMenuTypes.SHOP_MENU.get(), containerId);
        this.offers.addAll(shop.getOffers());
    }


    public List<ShopOffer> getOffers() {
        return offers;
    }

    public void setOffers(List<ShopOffer> newOffers) {
        offers.clear();
        offers.addAll(newOffers);
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
