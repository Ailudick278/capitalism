package com.ailudick.capitalismmod.market;

import com.ailudick.capitalismmod.Config;
import net.minecraft.core.BlockPos;

/** Converts world coordinates into stable, human-readable trade regions. */
public final class TradeRegion {
    private TradeRegion() {
    }

    public static String of(BlockPos pos) {
        int size = Config.TRADE_REGION_SIZE.get();
        return "r" + Math.floorDiv(pos.getX(), size) + "_" + Math.floorDiv(pos.getZ(), size);
    }

    public static int distance(String first, String second) {
        try {
            int[] a = parse(first);
            int[] b = parse(second);
            return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    private static int[] parse(String region) {
        if (region == null || !region.startsWith("r")) {
            throw new IllegalArgumentException("invalid region");
        }
        String[] parts = region.substring(1).split("_", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("invalid region");
        }
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }
}
