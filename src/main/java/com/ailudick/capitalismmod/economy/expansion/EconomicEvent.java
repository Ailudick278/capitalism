package com.ailudick.capitalismmod.economy.expansion;

import java.util.Objects;

/** A time-bounded economic event or contract-domain signal. Amounts use minor currency units. */
public record EconomicEvent(String id, ExpansionSystem system, String type, long createdAt,
                            long startsAt, long endsAt, EconomicActorRef source,
                            EconomicActorRef target, long amountMinor, String currencyId,
                            String status) {
    public EconomicEvent {
        id = Objects.requireNonNullElse(id, "").trim();
        type = Objects.requireNonNullElse(type, "").trim();
        currencyId = Objects.requireNonNullElse(currencyId, "").trim();
        status = Objects.requireNonNullElse(status, "active").trim();
        if (id.isEmpty() || system == null || type.isEmpty() || createdAt < 0L
                || startsAt < 0L || endsAt < startsAt || amountMinor < 0L) {
            throw new IllegalArgumentException("Invalid economic event");
        }
    }

    public boolean activeAt(long gameTime) {
        return gameTime >= startsAt && (endsAt == 0L || gameTime < endsAt) && "active".equals(status);
    }
}
