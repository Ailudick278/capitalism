package com.ailudick.capitalismmod.company;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreightContractAuditRulesTest {
    @Test
    void acceptsValidAcceptedContract() {
        assertTrue(FreightContractAuditRules.valid("f-1", "shipment-1", "buyer", "carrier",
                100L, 10L, 20L, 100L, "accepted"));
    }

    @Test
    void rejectsInvalidStatusAndTimeline() {
        assertFalse(FreightContractAuditRules.valid("f-1", "shipment-1", "buyer", "carrier",
                100L, 20L, 10L, 100L, "accepted"));
        assertFalse(FreightContractAuditRules.valid("f-1", "shipment-1", "buyer", "carrier",
                100L, 10L, 0L, 100L, "unknown"));
    }

    @Test
    void terminalStatusRequiresMatchingShipmentEvidence() {
        assertTrue(FreightContractAuditRules.terminalEvidence("settled", true, false));
        assertFalse(FreightContractAuditRules.terminalEvidence("settled", false, false));
        assertTrue(FreightContractAuditRules.terminalEvidence("loss", false, true));
        assertFalse(FreightContractAuditRules.terminalEvidence("loss", true, true));
    }
}
