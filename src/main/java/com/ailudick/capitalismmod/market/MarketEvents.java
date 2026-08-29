package com.ailudick.capitalismmod.market;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = CapitalismMod.MODID)
public class MarketEvents {

    /** Redeems any market payouts parked while the player was offline. */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MarketMailboxSavedData.get(player.getServer()).redeem(player);
        }
    }
}
