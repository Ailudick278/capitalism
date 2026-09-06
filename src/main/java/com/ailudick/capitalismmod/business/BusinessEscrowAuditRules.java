package com.ailudick.capitalismmod.business;

/** Pure invariants for individual-business order escrow batches. */
public final class BusinessEscrowAuditRules {
    private BusinessEscrowAuditRules() {
    }

    public static boolean validBatch(String batchId, int orderQuantity) {
        return validBatch(batchId, orderQuantity, 0);
    }

    public static boolean validBatch(String batchId, int orderQuantity, int batchQuantity) {
        if (batchId == null || batchId.isBlank() || orderQuantity <= 0) return false;
        try {
            int deliveredBefore = Integer.parseInt(batchId);
            return deliveredBefore >= 0 && deliveredBefore < orderQuantity
                    && (batchQuantity == 0 || batchQuantity > 0
                    && deliveredBefore <= orderQuantity - batchQuantity);
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
