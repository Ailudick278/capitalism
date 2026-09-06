package com.ailudick.capitalismmod.economy.expansion;

import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import com.ailudick.capitalismmod.market.Commodities;
import net.minecraft.server.MinecraftServer;

import net.minecraft.world.item.ItemStack;

/** Applies persistent, time-bounded economic-event modifiers to live systems. */
public final class EconomicEventService {
    private static final int AUTOMATIC_CORRECTION_BPS = 1_000;
    private static final int HIGH_PRICE_MULTIPLE_BPS = 25_000;
    private static final int LOW_PRICE_MULTIPLE_BPS = 4_000;
    private static final int AUTOMATIC_CORRECTION_DAYS = 3;

    private EconomicEventService() {}

    /**
     * Creates a bounded, deterministic correction event for an extreme market price.
     * The correction is deliberately weaker than the observed deviation so that
     * production, consumption and trade remain the primary price signals.
     */
    public static int createAutomaticMarketCorrections(MinecraftServer server, long gameTime) {
        if (server == null || gameTime < 0L) return 0;
        CommoditySavedData market = CommoditySavedData.get(server);
        int created = 0;
        long day = PerpetualCalendar.minecraftDayAtTicks(gameTime);
        for (ItemStack stack : Commodities.ALL) {
            String itemId = Commodities.id(stack);
            long fundamental = market.fundamental(itemId);
            long price = market.price(itemId);
            int shockBps = automaticCorrectionShockBps(price, fundamental);
            if (shockBps == 0 || hasActiveCommodityShock(server, itemId, gameTime)) continue;
            String direction = shockBps > 0 ? "up" : "down";
            String eventId = "auto-market-correction:" + day + ":" + direction + ":" + itemId;
            if (addCommodityPriceShock(server, eventId, itemId, shockBps, gameTime, AUTOMATIC_CORRECTION_DAYS)) {
                created++;
            }
        }
        return created;
    }

    /** Returns the stabilizing shock in basis points, or zero inside the normal band. */
    public static int automaticCorrectionShockBps(long price, long fundamental) {
        if (price <= 0L || fundamental <= 0L) return 0;
        if (price * 10_000L >= fundamental * HIGH_PRICE_MULTIPLE_BPS) return -AUTOMATIC_CORRECTION_BPS;
        if (price * 10_000L <= fundamental * LOW_PRICE_MULTIPLE_BPS) return AUTOMATIC_CORRECTION_BPS;
        return 0;
    }

    public static boolean addCommodityPriceShock(MinecraftServer server, String eventId, String itemId,
                                                  int shockBps, long startsAt, int durationDays) {
        if (server == null || eventId == null || eventId.isBlank() || itemId == null || itemId.isBlank()
                || shockBps < -9000 || shockBps > 9000 || startsAt < 0L || durationDays <= 0) return false;
        long endsAt = startsAt + PerpetualCalendar.ticksForDays(durationDays);
        if (endsAt <= startsAt) return false;
        String type = shockBps >= 0 ? "commodity_price_shock_up" : "commodity_price_shock_down";
        EconomicEvent event = new EconomicEvent(eventId, ExpansionSystem.ECONOMIC_EVENTS, type,
                server.overworld().getGameTime(), startsAt, endsAt, null,
                EconomicActorRef.of("commodity", itemId), Math.abs((long) shockBps), "bps", "active");
        return EconomicExpansionSavedData.get(server).addOnce(event);
    }

    public static boolean addLogisticsCapacityShock(MinecraftServer server, String eventId,
                                                     String origin, String destination, int shockBps,
                                                     long startsAt, int durationDays) {
        if (server == null || eventId == null || eventId.isBlank() || origin == null || origin.isBlank()
                || destination == null || destination.isBlank() || origin.equals(destination)
                || shockBps < -9000 || shockBps > 9000 || startsAt < 0L || durationDays <= 0) return false;
        long endsAt = startsAt + PerpetualCalendar.ticksForDays(durationDays);
        if (endsAt <= startsAt) return false;
        String type = shockBps >= 0 ? "logistics_capacity_shock_up" : "logistics_capacity_shock_down";
        EconomicEvent event = new EconomicEvent(eventId, ExpansionSystem.ECONOMIC_EVENTS, type,
                server.overworld().getGameTime(), startsAt, endsAt, null,
                EconomicActorRef.of("route", routeId(origin, destination)), Math.abs((long) shockBps), "bps", "active");
        return EconomicExpansionSavedData.get(server).addOnce(event);
    }

    public static int commodityPriceShockBps(MinecraftServer server, String itemId, long gameTime) {
        if (server == null || itemId == null || itemId.isBlank()) return 0;
        int total = 0;
        for (EconomicEvent event : EconomicExpansionSavedData.get(server).eventsFor(ExpansionSystem.ECONOMIC_EVENTS)) {
            if (!event.activeAt(gameTime) || event.target() == null || !itemId.equals(event.target().id())) continue;
            long signed = "commodity_price_shock_down".equals(event.type())
                    ? -event.amountMinor() : event.amountMinor();
            total = (int) Math.max(-9000L, Math.min(9000L, (long) total + signed));
        }
        return total;
    }

    public static int logisticsCapacityShockBps(MinecraftServer server, String origin, String destination, long gameTime) {
        if (server == null || origin == null || origin.isBlank() || destination == null || destination.isBlank()) return 0;
        int total = 0;
        String route = routeId(origin, destination);
        for (EconomicEvent event : EconomicExpansionSavedData.get(server).eventsFor(ExpansionSystem.ECONOMIC_EVENTS)) {
            if (!event.activeAt(gameTime) || event.target() == null || !route.equals(event.target().id())) continue;
            long signed = "logistics_capacity_shock_down".equals(event.type())
                    ? -event.amountMinor() : event.amountMinor();
            total = (int) Math.max(-9000L, Math.min(9000L, (long) total + signed));
        }
        return total;
    }

    public static int applyCapacityShock(int baseCapacity, int shockBps) {
        if (baseCapacity <= 0) return 0;
        long adjusted = (long) baseCapacity * (10_000L + Math.max(-9000, Math.min(9000, shockBps))) / 10_000L;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, adjusted));
    }

    public static long applyTravelShock(long baseTicks, int shockBps) {
        if (baseTicks <= 0L) return 1L;
        long multiplier = 10_000L - Math.max(-9000, Math.min(9000, shockBps)) / 2L;
        try {
            return Math.max(1L, Math.multiplyExact(baseTicks, multiplier) / 10_000L);
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }

    /** Applies route-capacity stress to a base disruption probability. */
    public static double applyLogisticsRiskShock(double baseRisk, int shockBps) {
        if (!Double.isFinite(baseRisk) || baseRisk <= 0D) return 0D;
        int bounded = Math.max(-9000, Math.min(9000, shockBps));
        double adjusted = baseRisk * (10_000D - bounded) / 10_000D;
        return Math.max(0D, Math.min(0.95D, adjusted));
    }

    private static String routeId(String origin, String destination) {
        return origin.trim() + "->" + destination.trim();
    }

    private static boolean hasActiveCommodityShock(MinecraftServer server, String itemId, long gameTime) {
        for (EconomicEvent event : EconomicExpansionSavedData.get(server).eventsFor(ExpansionSystem.ECONOMIC_EVENTS)) {
            if (event.activeAt(gameTime) && event.target() != null
                    && "commodity".equals(event.target().type()) && itemId.equals(event.target().id())
                    && event.type().startsWith("commodity_price_shock_")) return true;
        }
        return false;
    }

    public static int resolveExpired(MinecraftServer server, long gameTime) {
        if (server == null) return 0;
        int resolved = 0;
        EconomicExpansionSavedData data = EconomicExpansionSavedData.get(server);
        for (EconomicEvent event : data.events()) {
            if ("active".equals(event.status()) && event.endsAt() > 0L && gameTime >= event.endsAt()
                    && data.replace(new EconomicEvent(event.id(), event.system(), event.type(), event.createdAt(),
                    event.startsAt(), event.endsAt(), event.source(), event.target(), event.amountMinor(),
                    event.currencyId(), "resolved"))) resolved++;
        }
        return resolved;
    }
}
