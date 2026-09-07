package com.ailudick.capitalismmod.tax;

import java.util.UUID;

/** Pure consistency rules for carry-forward input-tax credits. */
public final class TaxCreditAuditRules {
    private TaxCreditAuditRules() {}

    public static boolean validLot(UUID taxpayerUuid, String currencyId, String subjectType,
                                   String subjectId, String sourceId, long periodStart, long periodEnd,
                                   long createdAt, long amount, boolean currencyExists) {
        return taxpayerUuid != null && currencyId != null && !currencyId.isBlank() && currencyExists
                && subjectType != null && !subjectType.isBlank() && subjectId != null && !subjectId.isBlank()
                && sourceId != null && !sourceId.isBlank() && periodStart >= 0L && periodEnd >= periodStart
                && createdAt >= 0L && amount > 0L;
    }

    public static boolean aggregateCovers(long aggregate, long lotTotal) {
        return aggregate >= 0L && lotTotal >= 0L && aggregate >= lotTotal;
    }
}
