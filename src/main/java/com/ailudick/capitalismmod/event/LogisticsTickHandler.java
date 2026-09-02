package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.market.LogisticsSavedData;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.currency.Money;
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
            double risk = Config.LOGISTICS_RISK_RATE.get()
                    * (1.0 - LogisticsInfrastructureSavedData.get(server).riskReduction(
                    shipment.originRegion(), shipment.destinationRegion(), shipment.transport()));
            if (risk > 0.0 && Math.random() < risk) {
                if (shipment.insured()) {
                    long payout;
                    try {
                        payout = Math.multiplyExact((long) shipment.quantity(), Config.LOGISTICS_DECLARED_VALUE.get());
                    } catch (ArithmeticException e) {
                        payout = Long.MAX_VALUE;
                    }
                    MarketMailboxSavedData.get(server).creditMoney(shipment.buyer(), "usd", Money.toMinor(payout));
                    data.remove(shipment.id());
                } else {
                    data.replace(new LogisticsSavedData.Shipment(shipment.id(), shipment.buyer(), shipment.itemId(),
                            shipment.quantity(), now + Config.LOGISTICS_DISRUPTION_TICKS.get(), shipment.originRegion(),
                            shipment.destinationRegion(), shipment.transport(), false));
                }
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
