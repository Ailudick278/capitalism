package com.ailudick.capitalismmod.economy.expansion;

import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import net.minecraft.server.MinecraftServer;

/** Applies persistent, time-bounded economic-event modifiers to live systems. */
public final class EconomicEventService {
    private EconomicEventService() {}

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
