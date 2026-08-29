package com.ailudick.capitalismmod.company;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Accrues company income for online players every {@link #INTERVAL} ticks.
 * Income only accrues while the owner is online, so per-player data suffices.
 */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public class CompanyTickHandler {
    private static final int INTERVAL = 600; // 600 ticks = 30 seconds
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < INTERVAL) {
            return;
        }
        tickCounter = 0;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            CompanyHelper.accrueIncome(player);
        }
    }
}
