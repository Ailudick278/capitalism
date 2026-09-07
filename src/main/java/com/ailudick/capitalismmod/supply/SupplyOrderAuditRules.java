package com.ailudick.capitalismmod.supply;

import java.util.Set;
import java.util.UUID;

/** Pure consistency rules for the append-only supply-order event stream. */
public final class SupplyOrderAuditRules {
    private static final Set<String> EVENT_TYPES = Set.of(
            "CREATED", "BACKORDERED", "PARTIAL", "FULFILLED", "DISPATCHED", "DELIVERED",
            "PARTIAL_LOSS", "LOST", "CANCELLED_REFUND", "EXPIRED_REFUND", "EXPIRED_REFUND_COMPANY",
            "INTENT_EXPIRED_REFUND", "INTENT_EXPIRED_REFUND_COMPANY");

    private SupplyOrderAuditRules() {}

    public static boolean validEvent(String orderId, String type, UUID buyer, UUID supplier,
                                     String itemId, int quantity, long amount, long occurredAt,
                                     boolean itemExists) {
        return orderId != null && !orderId.isBlank() && type != null && EVENT_TYPES.contains(type)
                && buyer != null && supplier != null && itemId != null && !itemId.isBlank() && itemExists
                && quantity > 0 && amount >= 0L && occurredAt >= 0L;
    }

    public static boolean sameOrderIdentity(UUID buyer, UUID supplier, String itemId,
                                            UUID expectedBuyer, UUID expectedSupplier, String expectedItemId) {
        return buyer != null && buyer.equals(expectedBuyer) && supplier != null && supplier.equals(expectedSupplier)
                && itemId != null && itemId.equals(expectedItemId);
    }

    public static boolean deliveryTotalsWithinOrder(long ordered, long delivered, long dispatched) {
        return ordered > 0L && delivered >= 0L && dispatched >= 0L
                && delivered <= ordered && dispatched <= ordered;
    }
}
