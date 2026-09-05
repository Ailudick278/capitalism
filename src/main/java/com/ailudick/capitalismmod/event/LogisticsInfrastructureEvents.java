package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.init.ModBlocks;
import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import com.ailudick.capitalismmod.market.LogisticsNodeSavedData;
import com.ailudick.capitalismmod.market.TradeRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Registers and unregisters player-built logistics facilities by trade region. */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class LogisticsInfrastructureEvents {
    private LogisticsInfrastructureEvents() {
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity() == null
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        String facility = facilityId(event.getPlacedBlock());
        if (facility != null) {
            LogisticsInfrastructureSavedData.get(event.getLevel().getServer())
                    .register(TradeRegion.of(event.getPos()), facility);
            LogisticsNodeSavedData.get(event.getLevel().getServer()).set(
                    level.dimension().location().toString(), event.getPos().getX(),
                    event.getPos().getY(), event.getPos().getZ(), facility);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        String facility = facilityId(event.getState());
        if (facility != null) {
            LogisticsInfrastructureSavedData.get(event.getLevel().getServer())
                    .unregister(TradeRegion.of(event.getPos()), facility);
            LogisticsNodeSavedData.get(event.getLevel().getServer()).remove(
                    level.dimension().location().toString(), event.getPos().getX(),
                    event.getPos().getY(), event.getPos().getZ());
        }
    }

    private static String facilityId(BlockState state) {
        if (state.is(ModBlocks.LOGISTICS_CENTER_BLOCK.get())) {
            return "logistics_center";
        }
        if (state.is(ModBlocks.TRANSFER_STATION_BLOCK.get())) {
            return "transfer_station";
        }
        if (state.is(ModBlocks.PORT_BLOCK.get())) {
            return "port";
        }
        return null;
    }
}
