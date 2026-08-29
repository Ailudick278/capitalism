package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CompanyMenu extends AbstractContainerMenu {
    private Map<String, Company> companies = new HashMap<>();

    public CompanyMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.COMPANY_MENU.get(), containerId);
    }

    public Map<String, Company> getCompanies() {
        return companies;
    }

    public void setCompanies(Map<String, Company> companies) {
        this.companies = companies;
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
