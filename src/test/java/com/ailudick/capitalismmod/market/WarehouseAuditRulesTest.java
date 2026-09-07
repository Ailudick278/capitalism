package com.ailudick.capitalismmod.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseAuditRulesTest {
    @Test
    void requiresSameItemAndQuantityForKnownReceipt() {
        assertTrue(WarehouseAuditRules.matchesConsumption("minecraft:wheat", 4,
                "minecraft:wheat", 4));
        assertFalse(WarehouseAuditRules.matchesConsumption("minecraft:wheat", 4,
                "minecraft:wheat", 3));
        assertFalse(WarehouseAuditRules.matchesConsumption("minecraft:wheat", 4,
                "minecraft:carrot", 4));
    }

    @Test
    void permitsLegacyReceiptWithoutMetadata() {
        assertTrue(WarehouseAuditRules.matchesConsumption(null, null, "minecraft:wheat", 4));
    }
}
