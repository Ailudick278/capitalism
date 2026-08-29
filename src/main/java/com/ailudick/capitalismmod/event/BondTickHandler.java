package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.bond.BondMarket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Ticks bond maturities once per Minecraft day.
 */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public class BondTickHandler {
    private static int dayCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        dayCounter++;
        if (dayCounter >= 24000) {
            dayCounter = 0;
            BondMarket.settleMaturity(event.getServer());
        }
    }
}
