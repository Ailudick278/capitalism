package com.ailudick.capitalismmod.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketOrderAuditRulesTest {
    @Test
    void validatesOrderIdentityAndEconomics() {
        assertTrue(MarketOrderAuditRules.valid("order-1", "player-1", "minecraft:wheat",
                4, 12L, true, 20L));
        assertFalse(MarketOrderAuditRules.valid("", "player-1", "minecraft:wheat",
                4, 12L, true, 20L));
        assertFalse(MarketOrderAuditRules.valid("order-1", "player-1", "minecraft:wheat",
                0, 12L, true, 20L));
    }

    @Test
    void acceptsOnlyAnIdentityPreservingPositiveFill() {
        assertTrue(MarketOrderAuditRules.validFill("order-1", "player-1", "minecraft:wheat", 4,
                12L, true, 20L, "order-1", "player-1", "minecraft:wheat", 2,
                12L, true, 20L, 2));
        assertFalse(MarketOrderAuditRules.validFill("order-1", "player-1", "minecraft:wheat", 4,
                12L, true, 20L, "order-1", "player-1", "minecraft:wheat", 5,
                12L, true, 20L, 2));
    }

    @Test
    void requiresBuyEscrowToCoverResidualOrder() {
        assertTrue(MarketOrderAuditRules.validBuyEscrow(1_000L, 800L));
        assertFalse(MarketOrderAuditRules.validBuyEscrow(799L, 800L));
        assertFalse(MarketOrderAuditRules.validBuyEscrow(1_000L, 0L));
    }

    @Test
    void originalSellEscrowMayCoverAPartiallyFilledOrder() {
        assertTrue(MarketOrderAuditRules.coversSellEscrow(10, 4));
        assertFalse(MarketOrderAuditRules.coversSellEscrow(3, 4));
        assertFalse(MarketOrderAuditRules.coversSellEscrow(null, 4));
    }
}
