package com.ailudick.capitalismmod.supply;

import java.util.Set;

/** Pure terminal-phase rules for supply delivery and loss settlement. */
public final class SupplySettlementAuditRules {
    private SupplySettlementAuditRules() {
    }

    public static boolean hasCompleteDelivery(Set<String> completedPhases) {
        return completedPhases != null
                && completedPhases.contains("inventory-debit")
                && completedPhases.contains("goods-dispatch")
                && completedPhases.contains("supplier-payout")
                && completedPhases.contains("escrow-release");
    }

    public static boolean hasCompleteLossRefund(Set<String> completedPhases) {
        return completedPhases != null && completedPhases.contains("loss-refund")
                && completedPhases.contains("loss-escrow-refund");
    }
}
