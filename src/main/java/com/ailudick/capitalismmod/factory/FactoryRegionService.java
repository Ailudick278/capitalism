package com.ailudick.capitalismmod.factory;

import com.ailudick.capitalismmod.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

/** Scans physical factory areas without coupling them to a particular transport mod. */
public final class FactoryRegionService {
    private FactoryRegionService() {}

    public record Scan(int warehouses, int inputPorts, int outputPorts, int machines, int logisticsNodes) {}

    public static Scan scan(ServerLevel level, FactoryRegionSavedData.Region region) {
        if (level == null || region == null || !level.dimension().location().toString().equals(region.dimension())) {
            return new Scan(0, 0, 0, 0, 0);
        }
        int warehouses = 0, inputPorts = 0, outputPorts = 0, machines = 0, logistics = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = region.minX(); x <= region.maxX(); x++) for (int y = region.minY(); y <= region.maxY(); y++)
            for (int z = region.minZ(); z <= region.maxZ(); z++) {
                cursor.set(x, y, z);
                Block block = level.getBlockState(cursor).getBlock();
                if (block == ModBlocks.WAREHOUSE_BLOCK.get()) warehouses++;
                if (block == ModBlocks.LOGISTICS_CENTER_BLOCK.get()
                        || block == ModBlocks.TRANSFER_STATION_BLOCK.get()
                        || block == ModBlocks.PORT_BLOCK.get()) logistics++;
                if (block == ModBlocks.FACTORY_BLOCK.get() || block == ModBlocks.FACTORY_MACHINE_BLOCK.get()) machines++;
                if (block == ModBlocks.FACTORY_INPUT_PORT_BLOCK.get()) inputPorts++;
                if (block == ModBlocks.FACTORY_OUTPUT_PORT_BLOCK.get()) outputPorts++;
            }
        return new Scan(warehouses, inputPorts, outputPorts, machines, logistics);
    }

    public static FactoryRegionSavedData.Region create(MinecraftServer server, ServerLevel level,
                                                         BlockPos center, String companyId, int radius) {
        if (server == null || level == null || center == null || companyId == null || companyId.isBlank()
                || radius < 4 || radius > 64) return null;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - 8);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + 24);
        FactoryRegionSavedData.Region region = new FactoryRegionSavedData.Region(
                FactoryRegionSavedData.newId(), companyId, level.dimension().location().toString(),
                center.getX() - radius, minY, center.getZ() - radius,
                center.getX() + radius, maxY, center.getZ() + radius);
        return FactoryRegionSavedData.get(server).add(region) ? region : null;
    }
}
