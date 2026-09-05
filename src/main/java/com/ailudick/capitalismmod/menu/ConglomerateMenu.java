package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.company.Conglomerate;
import com.ailudick.capitalismmod.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ConglomerateMenu extends AbstractContainerMenu {
    private String name = "";
    private Map<String, com.ailudick.capitalismmod.company.Company> companies = new HashMap<>();

    public ConglomerateMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.CONGLOMERATE_MENU.get(), containerId);
    }

    public Conglomerate getConglomerate() {
        return new Conglomerate(name, companies.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().companyId())));
    }

    public void setConglomerate(Conglomerate conglomerate) {
        this.name = conglomerate.name();
    }

    public String getName() {
        return name;
    }

    public Map<String, com.ailudick.capitalismmod.company.Company> getCompanies() {
        return companies;
    }

    public void setData(String name, Map<String, com.ailudick.capitalismmod.company.Company> companies) {
        this.name = name;
        this.companies = new HashMap<>(companies);
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
