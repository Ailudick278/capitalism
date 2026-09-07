package com.ailudick.capitalismmod.economy;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketTradeAuditRulesTest {
    private static final UUID BUYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SELLER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void acceptsPlayerTradeWithOneOrBothCounterparties() {
        assertTrue(MarketTradeAuditRules.valid(10L, BUYER, SELLER, "minecraft:wheat", 2,
                "usd", 100L, "commodity", 1L));
        assertTrue(MarketTradeAuditRules.valid(10L, BUYER, null, "minecraft:wheat", 2,
                "usd", 100L, "shop", 0L));
    }

    @Test
    void rejectsImpossibleTradeRecord() {
        assertFalse(MarketTradeAuditRules.valid(-1L, BUYER, SELLER, "minecraft:wheat", 2,
                "usd", 100L, "commodity", 1L));
        assertFalse(MarketTradeAuditRules.valid(10L, BUYER, BUYER, "minecraft:wheat", 2,
                "usd", 100L, "commodity", 1L));
        assertFalse(MarketTradeAuditRules.valid(10L, BUYER, SELLER, "minecraft:wheat", 2,
                "usd", 100L, "commodity", 101L));
    }
}
