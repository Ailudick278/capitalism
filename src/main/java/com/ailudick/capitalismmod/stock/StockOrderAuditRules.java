package com.ailudick.capitalismmod.stock;

/** Invariants for persisted stock orders and share escrow evidence. */
public final class StockOrderAuditRules {
    private StockOrderAuditRules() {
    }

    public static boolean valid(String id, String ownerId, String stockId, int quantity,
                                long pricePerUnit, long createdAt) {
        return id != null && !id.isBlank() && ownerId != null && !ownerId.isBlank()
                && stockId != null && !stockId.isBlank() && quantity > 0
                && pricePerUnit > 0L && createdAt >= 0L;
    }

    public static boolean coversSellEscrow(Long escrowedQuantity, int remainingQuantity) {
        return escrowedQuantity != null && escrowedQuantity > 0L
                && remainingQuantity > 0 && escrowedQuantity >= remainingQuantity;
    }
}
