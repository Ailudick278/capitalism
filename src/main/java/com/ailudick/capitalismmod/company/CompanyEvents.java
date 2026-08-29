package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.init.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = CapitalismMod.MODID)
public class CompanyEvents {

    /** Auto-creates a conglomerate for the player on first join. */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!player.hasData(ModAttachments.CONGLOMERATE.get())) {
            player.setData(ModAttachments.CONGLOMERATE.get(), Conglomerate.create(player.getName().getString()));
        }
    }
}
