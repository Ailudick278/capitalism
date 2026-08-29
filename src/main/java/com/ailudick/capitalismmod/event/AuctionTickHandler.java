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
        AuctionMarket.settleExpired(event.getServer());
    }
}
