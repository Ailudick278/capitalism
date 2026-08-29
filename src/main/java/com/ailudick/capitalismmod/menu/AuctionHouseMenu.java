package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.auction.Auction;
import com.ailudick.capitalismmod.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AuctionHouseMenu extends AbstractContainerMenu {
    private List<Auction> auctions = new ArrayList<>();

    public AuctionHouseMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.AUCTION_HOUSE_MENU.get(), containerId);
    }

    public List<Auction> getAuctions() {
        return auctions;
    }

    public void setAuctions(List<Auction> auctions) {
        this.auctions = auctions;
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
