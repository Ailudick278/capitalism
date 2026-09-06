package com.ailudick.capitalismmod.supply;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplySettlementAuditRulesTest {
    @Test
    void requiresAllDeliveryPhases() {
        assertTrue(SupplySettlementAuditRules.hasCompleteDelivery(Set.of(
                "inventory-debit", "goods-dispatch", "supplier-payout", "escrow-release")));
        assertFalse(SupplySettlementAuditRules.hasCompleteDelivery(Set.of(
                "inventory-debit", "goods-dispatch", "supplier-payout")));
    }

    @Test
    void requiresBothLossRefundPhases() {
        assertTrue(SupplySettlementAuditRules.hasCompleteLossRefund(Set.of(
                "loss-refund", "loss-escrow-refund")));
        assertFalse(SupplySettlementAuditRules.hasCompleteLossRefund(Set.of("loss-refund")));
    }
}
