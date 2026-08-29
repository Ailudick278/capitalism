package com.ailudick.capitalismmod.bank;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Settles deposit/loan interest for online players once per Minecraft day.
 */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public class BankTickHandler {
    private static int dayCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        dayCounter++;
        if (dayCounter >= 24000) { // 24000 ticks = 1 Minecraft day
            dayCounter = 0;
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                BankAccountHelper.applyDailyInterest(player);
            }
        }
    }
}
