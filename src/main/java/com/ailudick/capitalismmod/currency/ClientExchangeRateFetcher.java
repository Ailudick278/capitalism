package com.ailudick.capitalismmod.currency;

import com.ailudick.capitalismmod.CapitalismMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Fetches the same daily rates on the client so the bank UI matches the server. */
@EventBusSubscriber(modid = CapitalismMod.MODID, value = Dist.CLIENT)
public final class ClientExchangeRateFetcher {
    private static int tickCounter;
    private ClientExchangeRateFetcher() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ExchangeRateFetcher.tryFetchAsync();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (++tickCounter >= 6000) {
            tickCounter = 0;
            ExchangeRateFetcher.tryFetchAsync();
        }
    }
}
