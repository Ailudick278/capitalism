package com.ailudick.capitalismmod.market;

import java.util.Set;

/** Completion rules for the five durable phases of a commodity fill. */
public final class CommoditySettlementAuditRules {
    private static final Set<String> REQUIRED_PHASES = Set.of(
            "seller-payout", "goods-delivery", "tax", "volume", "order-update");

    private CommoditySettlementAuditRules() {
    }

    public static boolean complete(Set<String> completedPhases) {
        return completedPhases != null && completedPhases.containsAll(REQUIRED_PHASES);
    }
}
