package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Broadcasts news for large completed trades (shop, commodity exchange, or stock).
 */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public class NewsEvents {
    private static final long BIG_TRADE_THRESHOLD = 10000L; // USD

    @SubscribeEvent
    public static void onTradeCompleted(TradeCompletedEvent event) {
        if (event.getTotal() < BIG_TRADE_THRESHOLD) {
            return;
        }
        MinecraftServer server = event.getBuyer() != null ? event.getBuyer().getServer()
                : event.getSeller() != null ? event.getSeller().getServer() : null;
        if (server == null) {
            return;
        }
        EconomyNews.broadcast(server, "news.capitalismmod.big_trade",
                event.getQuantity(), event.getItem().getHoverName(), event.getTotal());
    }
}
