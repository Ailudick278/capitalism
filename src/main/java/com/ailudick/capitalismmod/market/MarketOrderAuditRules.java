package com.ailudick.capitalismmod.market;

/** Invariants for commodity limit orders and their partial-fill transitions. */
public final class MarketOrderAuditRules {
    private MarketOrderAuditRules() {
    }

    public static boolean valid(String id, String ownerId, String itemId, int quantity,
                                long pricePerUnit, boolean sell, long createdAt) {
        return id != null && !id.isBlank()
                && ownerId != null && !ownerId.isBlank()
                && itemId != null && !itemId.isBlank()
                && quantity > 0 && pricePerUnit > 0L && createdAt >= 0L;
    }

    public static boolean validFill(MarketOrder before, MarketOrder after, int fill) {
        return before != null && after != null && validFill(
                before.id(), before.ownerId(), before.commodity(), before.quantity(), before.pricePerUnit(),
                before.sell(), before.createdAt(), after.id(), after.ownerId(), after.commodity(),
                after.quantity(), after.pricePerUnit(), after.sell(), after.createdAt(), fill);
    }

    public static boolean sameIdentity(MarketOrder before, MarketOrder after) {
        return before != null && after != null
                && before.id().equals(after.id())
                && before.ownerId().equals(after.ownerId())
                && before.commodity().equals(after.commodity())
                && before.pricePerUnit() == after.pricePerUnit()
                && before.sell() == after.sell()
                && before.createdAt() == after.createdAt();
    }

    public static boolean validBuyEscrow(long paidMinor, long reservedMinor) {
        return paidMinor > 0L && reservedMinor > 0L && paidMinor >= reservedMinor;
    }

    public static boolean coversSellEscrow(Integer escrowedQuantity, int remainingQuantity) {
        return escrowedQuantity != null && escrowedQuantity > 0
                && remainingQuantity > 0 && escrowedQuantity >= remainingQuantity;
    }

    public static boolean validFill(String beforeId, String beforeOwnerId, Object beforeItem, int beforeQuantity,
                                    long beforePrice, boolean beforeSell, long beforeCreatedAt,
                                    String afterId, String afterOwnerId, Object afterItem, int afterQuantity,
                                    long afterPrice, boolean afterSell, long afterCreatedAt, int fill) {
        return fill > 0 && fill <= beforeQuantity
                && beforeId != null && beforeId.equals(afterId)
                && beforeOwnerId != null && beforeOwnerId.equals(afterOwnerId)
                && beforeItem != null && beforeItem.equals(afterItem)
                && beforePrice == afterPrice && beforeSell == afterSell
                && beforeCreatedAt == afterCreatedAt
                && afterQuantity == beforeQuantity - fill;
    }
}
