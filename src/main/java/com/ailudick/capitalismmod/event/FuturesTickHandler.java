package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.futures.FuturesMarket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Periodically updates futures prices and settles the trading day (mark-to-market,
 * expiry/rollover, forced liquidation).
 */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public class FuturesTickHandler {
    private static int tickCounter = 0;
    private static int dayCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter >= 600) { // 600 ticks = 30 seconds
            tickCounter = 0;
            FuturesMarket.updatePrices(event.getServer());
        }
        dayCounter++;
        if (dayCounter >= 24000) { // 24000 ticks = 1 Minecraft day
            dayCounter = 0;
            FuturesMarket.settleDay(event.getServer());
        }
    }
}
