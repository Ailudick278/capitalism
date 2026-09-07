package com.ailudick.capitalismmod.stock;

import java.util.Set;

/** Completion rules for the durable phases of a stock fill. */
public final class StockSettlementAuditRules {
    private static final Set<String> REQUIRED_PHASES = Set.of(
            "shares", "tax", "seller-payout", "volume", "order-update");

    private StockSettlementAuditRules() {
    }

    public static boolean complete(Set<String> completedPhases) {
        return completedPhases != null && completedPhases.containsAll(REQUIRED_PHASES);
    }
}
