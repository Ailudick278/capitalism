package com.ailudick.capitalismmod.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Validates the visible shell of a process unit around its controller. */
public final class MultiblockMachineService {
    private MultiblockMachineService() {}
    public static boolean isFormed(Level level, BlockPos controller, MultiblockMachineSpec spec) {
        int minX = controller.getX() - spec.width() / 2;
        int minZ = controller.getZ() - spec.depth() / 2;
        for (int y = 0; y < spec.height(); y++) for (int x = 0; x < spec.width(); x++) for (int z = 0; z < spec.depth(); z++) {
            BlockPos pos = new BlockPos(minX + x, controller.getY() + y, minZ + z);
            if (pos.equals(controller)) continue;
            boolean shell = x == 0 || x == spec.width() - 1 || z == 0 || z == spec.depth() - 1
                    || y == 0 || y == spec.height() - 1;
            if (shell) {
                var block = level.getBlockState(pos).getBlock();
                if (block != spec.casing() && block != spec.accent()) return false;
            } else if (!level.getBlockState(pos).isAir()) return false;
        }
        return true;
    }
}
