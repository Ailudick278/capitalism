package com.ailudick.capitalismmod.company;

/** Pure structural rules for company freight contracts. */
public final class FreightContractAuditRules {
    private FreightContractAuditRules() {}

    public static boolean valid(String id, String shipmentId, String buyerCompanyId, String carrierCompanyId,
                                long quotedCost, long createdAt, long acceptedAt, long expiresAt, String status) {
        if (id == null || id.isBlank() || shipmentId == null || shipmentId.isBlank()
                || buyerCompanyId == null || buyerCompanyId.isBlank()
                || carrierCompanyId == null || carrierCompanyId.isBlank() || quotedCost <= 0L
                || createdAt < 0L || (acceptedAt != 0L && acceptedAt < createdAt)
                || expiresAt < createdAt || status == null || status.isBlank()) return false;
        return switch (status) {
            case "offered", "accepted", "settled", "cancelled", "expired", "loss" -> true;
            default -> false;
        };
    }

    public static boolean terminalEvidence(String status, boolean delivered, boolean lost) {
        if ("settled".equals(status)) return delivered && !lost;
        if ("loss".equals(status)) return lost && !delivered;
        return true;
    }
}
