package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.menu.LandMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Client screen entry for the rebuilt land interface with an embedded map. */
public final class LandScreenAdapter extends LandScreen<LandMenu> {
    public LandScreenAdapter(LandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
