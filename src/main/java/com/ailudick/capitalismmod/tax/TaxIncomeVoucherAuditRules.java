package com.ailudick.capitalismmod.tax;

import java.util.UUID;

/** Pure structural checks for persisted taxable-income vouchers. */
public final class TaxIncomeVoucherAuditRules {
    private TaxIncomeVoucherAuditRules() {}

    public static boolean valid(UUID taxpayerUuid, String subjectId, String category, String currencyId,
                                long amount, long occurredAt, String sourceId, boolean currencyExists) {
        return taxpayerUuid != null && subjectId != null && !subjectId.isBlank()
                && category != null && !category.isBlank() && currencyId != null && !currencyId.isBlank()
                && currencyExists && amount > 0L && occurredAt >= 0L
                && sourceId != null && !sourceId.isBlank();
    }
}
