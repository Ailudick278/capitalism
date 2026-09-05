package com.ailudick.capitalismmod.supply;

import net.minecraft.server.MinecraftServer;

/** Centralized writer for supply-order lifecycle events. */
public final class SupplyOrderAuditService {
    private SupplyOrderAuditService() {
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
