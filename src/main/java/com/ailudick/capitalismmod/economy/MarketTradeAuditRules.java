package com.ailudick.capitalismmod.economy;

import java.util.UUID;

/** Invariants for the bounded persistent completed-trade history. */
public final class MarketTradeAuditRules {
    private MarketTradeAuditRules() {
    }

    public static boolean valid(long gameTime, UUID buyer, UUID seller, String itemId, int quantity,
                                String currencyId, long total, String market, long fee) {
        return gameTime >= 0L
                && (buyer != null || seller != null)
                && (buyer == null || seller == null || !buyer.equals(seller))
                && itemId != null && !itemId.isBlank() && quantity > 0
                && currencyId != null && !currencyId.isBlank()
                && total >= 0L && fee >= 0L && fee <= total
                && market != null && !market.isBlank();
    }
}
