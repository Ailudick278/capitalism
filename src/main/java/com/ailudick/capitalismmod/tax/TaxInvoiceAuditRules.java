package com.ailudick.capitalismmod.tax;

import java.util.UUID;

/** Pure structural rules for persisted VAT invoices. */
public final class TaxInvoiceAuditRules {
    private TaxInvoiceAuditRules() {}

    public static boolean valid(String id, String sourceEventId, UUID taxpayerUuid, String currencyId,
                                long grossAmount, long taxAmount, long creditApplied, long issuedAt,
                                String direction, boolean currencyExists) {
        return id != null && !id.isBlank() && sourceEventId != null && !sourceEventId.isBlank()
                && taxpayerUuid != null && currencyId != null && !currencyId.isBlank() && currencyExists
                && grossAmount > 0L && taxAmount >= 0L && creditApplied >= 0L && creditApplied <= taxAmount
                && issuedAt >= 0L && ("input".equals(direction) || "output".equals(direction));
    }
}
