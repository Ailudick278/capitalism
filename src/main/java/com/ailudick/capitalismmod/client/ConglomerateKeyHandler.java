package com.ailudick.capitalismmod.client;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.network.payload.OpenConglomeratePayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CapitalismMod.MODID, value = Dist.CLIENT)
public class ConglomerateKeyHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (ConglomerateKeyMapping.OPEN_CONGLOMERATE.consumeClick()) {
            PacketDistributor.sendToServer(new OpenConglomeratePayload());
        }
    }
}
