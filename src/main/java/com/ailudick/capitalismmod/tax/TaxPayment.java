package com.ailudick.capitalismmod.tax;

import java.util.UUID;

/** Immutable record of one successful tax payment. Amounts use minor currency units. */
public record TaxPayment(String id, String billId, UUID taxpayerUuid, String currencyId,
                         long amount, long paidAt) {
    public TaxPayment {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Payment id is required");
        if (billId == null || billId.isBlank()) throw new IllegalArgumentException("Bill id is required");
        if (taxpayerUuid == null) throw new IllegalArgumentException("Taxpayer is required");
        if (amount <= 0L) throw new IllegalArgumentException("Payment amount must be positive");
    }
}
