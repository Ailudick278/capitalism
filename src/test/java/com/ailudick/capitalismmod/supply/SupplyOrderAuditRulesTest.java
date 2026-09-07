package com.ailudick.capitalismmod.supply;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplyOrderAuditRulesTest {
    private static final UUID BUYER = UUID.randomUUID();
    private static final UUID SUPPLIER = UUID.randomUUID();

    @Test
    void acceptsKnownEventWithValidFields() {
        assertTrue(SupplyOrderAuditRules.validEvent("order-1", "DELIVERED", BUYER, SUPPLIER,
                "minecraft:iron_ingot", 4, 80L, 10L, true));
    }

    @Test
    void rejectsUnknownOrMalformedEvent() {
        assertFalse(SupplyOrderAuditRules.validEvent("order-1", "MADE_UP", BUYER, SUPPLIER,
                "minecraft:iron_ingot", 4, 80L, 10L, true));
        assertFalse(SupplyOrderAuditRules.validEvent("order-1", "DELIVERED", BUYER, SUPPLIER,
                "minecraft:iron_ingot", 0, 80L, 10L, true));
        assertFalse(SupplyOrderAuditRules.validEvent("order-1", "DELIVERED", BUYER, SUPPLIER,
                "minecraft:missing", 4, 80L, 10L, false));
    }

    @Test
    void enforcesIdentityAndQuantityBounds() {
        assertTrue(SupplyOrderAuditRules.sameOrderIdentity(BUYER, SUPPLIER, "minecraft:iron_ingot",
                BUYER, SUPPLIER, "minecraft:iron_ingot"));
        assertFalse(SupplyOrderAuditRules.sameOrderIdentity(BUYER, SUPPLIER, "minecraft:gold_ingot",
                BUYER, SUPPLIER, "minecraft:iron_ingot"));
        assertTrue(SupplyOrderAuditRules.deliveryTotalsWithinOrder(10L, 6L, 10L));
        assertFalse(SupplyOrderAuditRules.deliveryTotalsWithinOrder(10L, 11L, 0L));
    }
}
