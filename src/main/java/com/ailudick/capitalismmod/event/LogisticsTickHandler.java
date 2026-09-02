package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.market.LogisticsSavedData;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;

/** Delivers due cargo into persistent warehouses, including for offline buyers. */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class LogisticsTickHandler {
    private static final int CHECK_INTERVAL = 20;
    private static int tickCounter;

    private LogisticsTickHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;
        MinecraftServer server = event.getServer();
        LogisticsSavedData data = LogisticsSavedData.get(server);
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        long now = server.overworld().getGameTime();
        for (LogisticsSavedData.Shipment shipment : new ArrayList<>(data.shipments())) {
            if (shipment.deliveryTick() > now) {
                continue;
            }
            Item item = parseItem(shipment.itemId());
            if (item != null) {
                warehouse.credit(shipment.buyer(), item, shipment.quantity());
            }
            data.remove(shipment.id());
        }
    }

    private static Item parseItem(String itemId) {
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            return item == null || item == Items.AIR ? null : item;
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
