package com.ailudick.capitalismmod.supply;

/** Pure invariants for supply escrow creation and persisted balances. */
public final class SupplyEscrowAuditRules {
    private SupplyEscrowAuditRules() {
    }

    public static boolean creationMatches(SupplyEscrowSavedData.Escrow existing,
                                          int quantity, long originalMinor) {
        return existing != null && quantity >= 0 && originalMinor > 0L
                && existing.originalMinor() == originalMinor
                && (existing.quantity() == 0 || quantity == 0 || existing.quantity() == quantity);
    }
}
