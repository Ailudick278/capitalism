package com.ailudick.capitalismmod.bank;

import com.ailudick.capitalismmod.CapitalismMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Settles deposit/loan interest for online players once per Minecraft day.
 */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public class BankTickHandler {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
    }
}
