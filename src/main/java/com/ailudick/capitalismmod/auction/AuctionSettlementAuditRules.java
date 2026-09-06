package com.ailudick.capitalismmod.auction;

import java.util.Set;

/** Pure terminal-state rules for auction settlement journals. */
public final class AuctionSettlementAuditRules {
    private AuctionSettlementAuditRules() {
    }

    public static boolean hasCompleteOutcome(Set<String> completedPhases) {
        if (completedPhases == null) return false;
        return completedPhases.contains("goods-return")
                || (completedPhases.contains("goods-delivery")
                && completedPhases.contains("seller-payout")
                && completedPhases.contains("sale-tax"));
    }
}
