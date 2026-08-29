package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SecuritiesCommissionMenu extends AbstractContainerMenu {
    private Map<String, Company> companies = new HashMap<>();
    private Set<String> listed = new HashSet<>();

    public SecuritiesCommissionMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.SECURITIES_COMMISSION_MENU.get(), containerId);
    }

    public Map<String, Company> getCompanies() {
        return companies;
    }

    public void setCompanies(Map<String, Company> companies) {
        this.companies = companies;
    }

    public boolean isListed(String name) {
        return listed.contains(name);
    }

    public Set<String> getListed() {
        return listed;
    }

    public void setListed(Set<String> listed) {
        this.listed = listed;
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
