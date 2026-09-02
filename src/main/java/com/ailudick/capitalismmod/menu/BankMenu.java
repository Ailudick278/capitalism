package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.bank.BankAccount;
import com.ailudick.capitalismmod.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class BankMenu extends AbstractContainerMenu {
    private Map<String, BankAccount> accounts = new HashMap<>();
    private Map<String, Long> personalAssets = new HashMap<>();

    public BankMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.BANK_MENU.get(), containerId);
    }

    public Map<String, BankAccount> getAccounts() {
        return accounts;
    }

    public void setAccounts(Map<String, BankAccount> accounts) {
        this.accounts = accounts;
    }

    public Map<String, Long> getPersonalAssets() {
        return personalAssets;
    }

    public void setPersonalAssets(Map<String, Long> personalAssets) {
        this.personalAssets = personalAssets;
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
