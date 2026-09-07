package com.ailudick.capitalismmod.market;

import org.junit.jupiter.api.Test;

import java.util.Map;

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

    @Test
    void batchRetryMustMatchMaterialSignature() {
        Map<String, Integer> batch = Map.of("minecraft:iron_ingot", 2, "minecraft:stick", 4);
        String signature = WarehouseAuditRules.batchSignature(batch);
        assertTrue(WarehouseAuditRules.matchesBatch(signature, batch));
        assertFalse(WarehouseAuditRules.matchesBatch(signature, Map.of("minecraft:iron_ingot", 1)));
        assertTrue(WarehouseAuditRules.matchesBatch(null, batch));
    }
}
