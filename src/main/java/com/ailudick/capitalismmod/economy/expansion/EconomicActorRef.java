package com.ailudick.capitalismmod.economy.expansion;

import java.util.Objects;

/** Stable reference to a household, company, institution, government or external actor. */
public record EconomicActorRef(String type, String id) {
    public EconomicActorRef {
        type = Objects.requireNonNullElse(type, "").trim();
        id = Objects.requireNonNullElse(id, "").trim();
        if (type.isEmpty() || id.isEmpty()) {
            throw new IllegalArgumentException("Economic actor type and id are required");
        }
    }

    public static EconomicActorRef of(String type, String id) {
        return new EconomicActorRef(type, id);
    }
}
