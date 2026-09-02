package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.stock.StockMarket;
import com.ailudick.capitalismmod.economy.EconomySavedData;
import com.ailudick.capitalismmod.network.payload.SyncStocksPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.HashMap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Periodically applies supply/demand price updates to the stock market.
 */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public class StockTickHandler {
    private static int tickCounter = 0;
    private static int testTickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        testTickCounter++;
        if (testTickCounter >= 20) { // one update per second
            testTickCounter = 0;
            StockMarket.updateTestStock(event.getServer());
            syncPlayers(event.getServer());
        }
        if (tickCounter >= 600) { // 600 ticks = 30 seconds
            tickCounter = 0;
            StockMarket.updatePrices(event.getServer());
            syncPlayers(event.getServer());
        }
    }

    private static void syncPlayers(net.minecraft.server.MinecraftServer server) {
        EconomySavedData data = EconomySavedData.get(server);
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, new SyncStocksPayload(
                    new HashMap<>(data.prices()),
                    new HashMap<>(data.portfolio(player.getUUID())),
                    new HashMap<>(data.history()),
                    new HashMap<>(data.listingNames())));
        }
    }
}
