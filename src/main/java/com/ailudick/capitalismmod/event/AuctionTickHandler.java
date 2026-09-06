package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.auction.AuctionMarket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Settles expiring auctions every tick.
 */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public class AuctionTickHandler {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Listing intents can exist before any player reconnects after a restart.
        // Retry them periodically so offline servers do not strand escrowed goods.
        if (event.getServer().overworld().getGameTime() % 20L == 0L) {
            AuctionMarket.recoverListingIntents(event.getServer());
        }
        AuctionMarket.settleExpired(event.getServer());
    }
}
