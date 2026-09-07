package com.ailudick.capitalismmod.bank;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Invariants for the persisted central-bank facility ledger. */
public final class CentralBankFacilityAuditRules {
    private CentralBankFacilityAuditRules() {}

    public static boolean validHistory(List<CentralBankFacilitySavedData.Facility> facilities) {
        if (facilities == null) return false;
        Set<String> ids = new HashSet<>();
        for (var facility : facilities) {
            if (facility == null || facility.id() == null || facility.id().isBlank()
                    || !ids.add(facility.id()) || facility.issuedDay() < 0L
                    || !CentralBankFacilityEconomics.validTerms(facility.principalMinor(),
                    facility.annualRateBasisPoints(), facility.termDays())
                    || facility.remainingPrincipal() < 0L
                    || facility.remainingPrincipal() > facility.principalMinor()
                    || facility.daysRemaining() < 0 || facility.daysRemaining() > facility.termDays()
                    || facility.lastSettlementDay() < -1L) return false;
        }
        return true;
    }
}
