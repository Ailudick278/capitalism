package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.economy.MarketTradeSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Persists completed trade events for audit and market statistics. */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class MarketTradeEvents {
    private MarketTradeEvents() {
    }

    @SubscribeEvent
    public static void onTradeCompleted(TradeCompletedEvent event) {
        Player buyer = event.getBuyer();
        Player seller = event.getSeller();
        MinecraftServer server = buyer != null ? buyer.getServer() : seller != null ? seller.getServer() : null;
        if (server == null || event.getQuantity() <= 0 || event.getTotal() < 0) {
            return;
        }
        String itemId = event.getAssetId();
        if (itemId == null && event.getItem() != null && !event.getItem().isEmpty()) {
            itemId = BuiltInRegistries.ITEM.getKey(event.getItem().getItem()).toString();
        }
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        MarketTradeSavedData.get(server).add(new MarketTradeSavedData.Trade(
                server.overworld().getGameTime(),
                buyer == null ? null : buyer.getUUID(),
                seller == null ? null : seller.getUUID(),
                itemId, event.getQuantity(), event.getCurrencyId(), event.getTotal(), event.getMarket(), event.getFee()));
    }
}
