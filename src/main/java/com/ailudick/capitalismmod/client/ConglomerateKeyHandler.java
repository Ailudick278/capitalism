package com.ailudick.capitalismmod.client;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.network.payload.OpenWorldMapPayload;
import com.ailudick.capitalismmod.network.payload.OpenLandPayload;
import com.ailudick.capitalismmod.network.payload.RequestWorldMapTilesPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CapitalismMod.MODID, value = Dist.CLIENT)
public class ConglomerateKeyHandler {
    private static int lastDiscoveryChunkX = Integer.MIN_VALUE;
    private static int lastDiscoveryChunkZ = Integer.MIN_VALUE;
    private static String lastDiscoveryDimension = "";

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (ConglomerateKeyMapping.OPEN_LAND.consumeClick()) {
            PacketDistributor.sendToServer(new OpenWorldMapPayload());
        }
        while (ConglomerateKeyMapping.OPEN_LAND_MENU.consumeClick()) {
            PacketDistributor.sendToServer(new OpenLandPayload());
        }
        if (Minecraft.getInstance().player != null) {
            var player = Minecraft.getInstance().player;
            var chunk = player.chunkPosition();
            String dimension = player.level().dimension().location().toString();
            if (chunk.x != lastDiscoveryChunkX || chunk.z != lastDiscoveryChunkZ
                    || !dimension.equals(lastDiscoveryDimension)) {
                PacketDistributor.sendToServer(new RequestWorldMapTilesPayload(chunk.x, chunk.z,
                        Config.WORLD_MAP_DISCOVERY_RADIUS.get(), true));
                lastDiscoveryChunkX = chunk.x;
                lastDiscoveryChunkZ = chunk.z;
                lastDiscoveryDimension = dimension;
            }
        } else {
            lastDiscoveryChunkX = Integer.MIN_VALUE;
            lastDiscoveryChunkZ = Integer.MIN_VALUE;
            lastDiscoveryDimension = "";
        }
    }
}
