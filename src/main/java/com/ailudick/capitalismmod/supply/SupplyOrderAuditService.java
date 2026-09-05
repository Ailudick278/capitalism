package com.ailudick.capitalismmod.supply;

import net.minecraft.server.MinecraftServer;

/** Centralized writer for supply-order lifecycle events. */
public final class SupplyOrderAuditService {
    private SupplyOrderAuditService() {
    }

    /** Resolves the operational status from the immutable order event stream. */
    public static String currentStatus(MinecraftServer server, String orderId) {
        if (server == null || orderId == null || orderId.isBlank()) return "UNKNOWN";
        java.util.List<SupplyOrderAuditSavedData.Event> events =
                SupplyOrderAuditSavedData.get(server).forOrder(orderId);
        if (events.isEmpty()) return "UNKNOWN";
        if (events.stream().anyMatch(event -> "LOST".equals(event.type()))) return "LOST";
        if (events.stream().anyMatch(event -> event.type().startsWith("EXPIRED_REFUND"))) return "REFUNDED";

        long ordered = events.stream().filter(event -> "CREATED".equals(event.type()))
                .mapToLong(SupplyOrderAuditSavedData.Event::quantity).max().orElse(0L);
        long received = events.stream().filter(event -> "DELIVERED".equals(event.type()))
                .mapToLong(SupplyOrderAuditSavedData.Event::quantity).sum();
        long dispatched = events.stream().filter(event -> "DISPATCHED".equals(event.type()))
                .mapToLong(SupplyOrderAuditSavedData.Event::quantity).sum();
        if (ordered > 0L && received >= ordered) return "RECEIVED";
        if (dispatched > 0L) return "IN_TRANSIT";
        if (events.stream().anyMatch(event -> "BACKORDERED".equals(event.type())
                || "PARTIAL".equals(event.type()))) return "BACKORDERED";
        if (events.stream().anyMatch(event -> "FULFILLED".equals(event.type()))) return "SUPPLIER_FULFILLED";
        if (events.stream().anyMatch(event -> "CREATED".equals(event.type()))) return "PLACED";
        return "UNKNOWN";
    }

    public static void record(MinecraftServer server, PurchaseOrder order, String type, int quantity, long amount) {
        if (server == null || order == null) return;
        SupplyOrderAuditSavedData.get(server).append(new SupplyOrderAuditSavedData.Event(
                order.id(), type, order.buyerUuid(), order.supplierUuid(), order.itemId(), quantity,
                Math.max(0L, amount), server.overworld().getGameTime()));
    }

    public static void record(MinecraftServer server, String orderId, String type, java.util.UUID buyerUuid,
                              java.util.UUID supplierUuid, String itemId, int quantity, long amount) {
        if (server == null || orderId == null || orderId.isBlank() || buyerUuid == null || supplierUuid == null) return;
        SupplyOrderAuditSavedData.get(server).append(new SupplyOrderAuditSavedData.Event(
                orderId, type, buyerUuid, supplierUuid, itemId == null ? "" : itemId, quantity,
                Math.max(0L, amount), server.overworld().getGameTime()));
    }
}
