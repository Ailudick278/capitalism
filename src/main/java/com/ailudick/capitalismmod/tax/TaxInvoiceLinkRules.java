package com.ailudick.capitalismmod.tax;

import java.util.UUID;

/** Pure rules linking VAT invoices with the tax bill created from the same source. */
public final class TaxInvoiceLinkRules {
    private TaxInvoiceLinkRules() {}

    public static boolean matchesBill(String direction, long grossAmount, long taxAmount, long creditApplied,
                                      UUID taxpayerUuid, String currencyId, String sourceEventId,
                                      TaxBill bill) {
        if ("input".equals(direction)) {
            return creditApplied == taxAmount && bill == null;
        }
        if (!"output".equals(direction)) return false;
        if (bill == null) return creditApplied == taxAmount;
        return taxpayerUuid != null && taxpayerUuid.equals(bill.subject().taxpayerUuid())
                && currencyId != null && currencyId.equals(bill.currencyId())
                && sourceEventId != null && sourceEventId.equals(bill.sourceEventId())
                && bill.taxableBase() == grossAmount
                && bill.amount() == taxAmount - creditApplied;
    }
}
