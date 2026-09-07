package com.ailudick.capitalismmod.company;

/** Pure invariants for freight payable and settlement phase records. */
public final class FreightSettlementAuditRules {
    private FreightSettlementAuditRules() {}

    public static boolean validCost(String shipmentId, String companyId, String itemId,
                                    int quantity, long estimatedCost, long appliedAt,
                                    boolean settled, String carrierCompanyId, long settledAt) {
        return shipmentId != null && !shipmentId.isBlank() && companyId != null && !companyId.isBlank()
                && itemId != null && !itemId.isBlank() && quantity > 0 && estimatedCost > 0L && appliedAt >= 0L
                && (!settled || (carrierCompanyId != null && !carrierCompanyId.isBlank()
                && settledAt >= appliedAt));
    }

    public static boolean quoteMatchesCost(long quotedCost, long estimatedCost) {
        return quotedCost > 0L && estimatedCost > 0L && quotedCost == estimatedCost;
    }

    public static boolean validSettlement(String shipmentId, String buyerCompanyId, String carrierCompanyId,
                                          long amount, boolean buyerDebited, boolean carrierCredited,
                                          boolean payableClosed) {
        return shipmentId != null && !shipmentId.isBlank() && buyerCompanyId != null && !buyerCompanyId.isBlank()
                && carrierCompanyId != null && !carrierCompanyId.isBlank() && !buyerCompanyId.equals(carrierCompanyId)
                && amount > 0L && (!carrierCredited || buyerDebited)
                && (!payableClosed || (buyerDebited && carrierCredited));
    }
}
