package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.worldmap.WorldMapTileSavedData;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Invalidates explored map tiles when players modify the world. */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class WorldMapEvents {
    private WorldMapEvents() {}

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            invalidateAround(level, event.getPos().getX() >> 4, event.getPos().getZ() >> 4);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            var pos = event.getBlockSnapshot().getPos();
            invalidateAround(level, pos.getX() >> 4, pos.getZ() >> 4);
        }
    }

    private static void invalidateAround(ServerLevel level, int chunkX, int chunkZ) {
        WorldMapTileSavedData cache = WorldMapTileSavedData.get(level.getServer());
        String dimension = level.dimension().location().toString();
        for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
            for (int x = chunkX - 1; x <= chunkX + 1; x++) {
                cache.invalidate(dimension + ":" + x + ":" + z);
            }
        }
    }
}
