package com.ailudick.capitalismmod.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessEscrowAuditRulesTest {
    @Test
    void validatesDeliveryBeforeBatch() {
        assertTrue(BusinessEscrowAuditRules.validBatch("0", 10));
        assertTrue(BusinessEscrowAuditRules.validBatch("9", 10));
        assertTrue(BusinessEscrowAuditRules.validBatch("4", 10, 6));
        assertFalse(BusinessEscrowAuditRules.validBatch("10", 10));
        assertFalse(BusinessEscrowAuditRules.validBatch("5", 10, 6));
        assertFalse(BusinessEscrowAuditRules.validBatch("batch-a", 10));
    }
}
