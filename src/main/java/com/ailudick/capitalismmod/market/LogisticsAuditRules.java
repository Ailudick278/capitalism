package com.ailudick.capitalismmod.market;

import java.util.Set;
import java.util.UUID;

/** Pure validation rules for the three durable logistics settlement ledgers. */
public final class LogisticsAuditRules {
    private LogisticsAuditRules() {
    }

    public static boolean claimUnique(Set<String> ids, String shipmentId) {
        return shipmentId != null && !shipmentId.isBlank() && ids.add(shipmentId);
    }

    public static boolean validShipment(String id, UUID buyer, String itemId, int quantity,
                                        long deliveryTick, TransportMode transport,
                                        int disruptionCount, long unitPrice) {
        return id != null && !id.isBlank() && buyer != null && itemId != null && !itemId.isBlank()
                && quantity > 0 && deliveryTick >= 0L && transport != null
                && disruptionCount >= 0 && unitPrice >= 0L;
    }

    public static boolean validDelivery(String id, UUID buyer, String itemId, int quantity, long deliveredAt) {
        return id != null && !id.isBlank() && buyer != null && itemId != null && !itemId.isBlank()
                && quantity > 0 && deliveredAt >= 0L;
    }

    public static boolean validLoss(String id, UUID buyer, String itemId, int quantity,
                                    TransportMode transport, int disruptionCount, long lostAt, long unitPrice) {
        return id != null && !id.isBlank() && buyer != null && itemId != null && !itemId.isBlank()
                && quantity > 0 && transport != null && disruptionCount >= 0
                && lostAt >= 0L && unitPrice >= 0L;
    }
}
