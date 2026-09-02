package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.market.CommodityMarket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Periodically applies supply/demand price updates to the commodity market.
 */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public class CommodityTickHandler {
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter >= 600) { // 600 ticks = 30 seconds
            tickCounter = 0;
            CommodityMarket.updatePrices(event.getServer());
        }
    }
}
