package com.ailudick.capitalismmod.market;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogisticsAuditRulesTest {
    private static final UUID BUYER = UUID.randomUUID();

    @Test
    void acceptsOneValidSettlementRecordAndRejectsDuplicateTerminalState() {
        var ids = new HashSet<String>();
        assertTrue(LogisticsAuditRules.validShipment("shipment-1", BUYER, "minecraft:wheat", 4,
                20L, TransportMode.ROAD, 0, 12L));
        assertTrue(LogisticsAuditRules.claimUnique(ids, "shipment-1"));
        assertFalse(LogisticsAuditRules.claimUnique(ids, "shipment-1"));
    }

    @Test
    void rejectsMalformedDeliveryAndLossRecords() {
        assertFalse(LogisticsAuditRules.validDelivery("", BUYER, "minecraft:wheat", 1, 10L));
        assertFalse(LogisticsAuditRules.validDelivery("shipment-1", BUYER, "minecraft:wheat", 0, 10L));
        assertFalse(LogisticsAuditRules.validLoss("shipment-1", BUYER, "minecraft:wheat", 1,
                null, 0, 10L, 1L));
        assertFalse(LogisticsAuditRules.validLoss("shipment-1", BUYER, "minecraft:wheat", 1,
                TransportMode.ROAD, 0, -1L, 1L));
    }
}
