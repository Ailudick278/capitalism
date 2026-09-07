package com.ailudick.capitalismmod.market;

/** Safety invariants for persisted daily commodity flow accumulators. */
public final class CommodityFlowAuditRules {
    private static final long MAX_DAILY_FLOW = 1_000_000_000_000L;

    private CommodityFlowAuditRules() {
    }

    public static boolean valid(String itemId, long flow) {
        return itemId != null && !itemId.isBlank()
                && flow >= -MAX_DAILY_FLOW && flow <= MAX_DAILY_FLOW;
    }
}
