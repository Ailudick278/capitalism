package com.ailudick.capitalismmod.land;

import com.ailudick.capitalismmod.Config;
import net.minecraft.world.level.Level;

/** Calculates a transparent reference price; it does not control seller pricing. */
public final class LandValuationHelper {
    private LandValuationHelper() {}

    public static long suggestedPrice(Level level, LandClaim claim) {
        double purposeFactor = switch (claim.purpose()) {
            case "residential", "2301" -> 1.20;
            case "commercial", "04" -> 1.35;
            case "industrial", "03" -> 1.10;
            case "agriculture", "01" -> 1.05;
            case "mining", "02" -> 1.25;
            case "public", "06" -> 0.90;
            default -> 1.0;
        };
        double distance = Math.sqrt(Math.pow(claim.chunkX() * 16.0 - level.getSharedSpawnPos().getX(), 2)
                + Math.pow(claim.chunkZ() * 16.0 - level.getSharedSpawnPos().getZ(), 2));
        double locationFactor = Math.max(0.75, Math.min(1.50, 1.50 - distance / 20000.0));
        double value = (Config.LAND_CLAIM_PRICE.get() + Math.max(0L, claim.resourceAmount()) * 5.0)
                * purposeFactor * locationFactor;
        return Math.max(0L, Math.round(value));
    }
}
