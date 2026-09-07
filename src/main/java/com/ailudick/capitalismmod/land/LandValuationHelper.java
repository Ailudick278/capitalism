package com.ailudick.capitalismmod.land;

import com.ailudick.capitalismmod.Config;
import net.minecraft.world.level.Level;
import net.minecraft.server.MinecraftServer;
import com.ailudick.capitalismmod.market.TradeRegion;
import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import com.ailudick.capitalismmod.population.PopulationSavedData;
import com.ailudick.capitalismmod.company.CompanySiteSavedData;

/** Calculates a transparent reference price; it does not control seller pricing. */
public final class LandValuationHelper {
    private LandValuationHelper() {}

    public static long suggestedPrice(Level level, LandClaim claim) {
        return suggestedPrice(level, claim, 0, 50);
    }

    /** Server-aware valuation incorporating regional population and services. */
    public static long suggestedPrice(MinecraftServer server, LandClaim claim) {
        if (server == null || claim == null) return 0L;
        String region = TradeRegion.of(new net.minecraft.core.BlockPos(claim.chunkX() * 16, 0, claim.chunkZ() * 16));
        int residents = PopulationSavedData.get(server).population(region);
        LogisticsInfrastructureSavedData infrastructure = LogisticsInfrastructureSavedData.get(server);
        int services = infrastructure.publicServiceScore(server, region, residents);
        int access = infrastructure.accessScore(region);
        int businessSites = (int) CompanySiteSavedData.get(server).allSites().stream()
                .filter(site -> claim.dimension().equals(site.dimension()))
                .filter(site -> region.equals(TradeRegion.of(new net.minecraft.core.BlockPos(
                site.chunkX() * 16, 0, site.chunkZ() * 16)))).count();
        long marketAverage = LandMarketSavedData.get(server).average(claim.dimension());
        return suggestedPrice(server.overworld(), claim, residents, services, businessSites, access, marketAverage);
    }

    private static long suggestedPrice(Level level, LandClaim claim, int residents, int services) {
        return suggestedPrice(level, claim, residents, services, 0);
    }

    private static long suggestedPrice(Level level, LandClaim claim, int residents, int services,
                                       int businessSites) {
        return suggestedPrice(level, claim, residents, services, businessSites, 50);
    }

    private static long suggestedPrice(Level level, LandClaim claim, int residents, int services,
                                       int businessSites, int access) {
        return suggestedPrice(level, claim, residents, services, businessSites, access, 0L);
    }

    private static long suggestedPrice(Level level, LandClaim claim, int residents, int services,
                                       int businessSites, int access, long marketAverage) {
        if (level == null || claim == null) return 0L;
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
        double baseline = Config.LAND_CLAIM_PRICE.get() + Math.max(0L, claim.resourceAmount()) * 5.0;
        double value = baseline
                * purposeFactor * locationFactor * LandDemandEconomics.multiplier(
                residents, services, businessSites, access)
                * LandDemandEconomics.marketMultiplier(marketAverage, baseline);
        return Math.max(0L, Math.round(value));
    }
}
