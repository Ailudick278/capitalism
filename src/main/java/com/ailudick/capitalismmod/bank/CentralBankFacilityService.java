package com.ailudick.capitalismmod.bank;

import com.ailudick.capitalismmod.government.GovernmentPolicySavedData;
import com.ailudick.capitalismmod.risk.FinancialCrisisSavedData;
import net.minecraft.server.MinecraftServer;

/** Issues central-bank liquidity facilities from the fiscal reserve during a crisis. */
public final class CentralBankFacilityService {
    private CentralBankFacilityService() {}

    public static boolean issueFromTreasury(MinecraftServer server, long day, long amountMinor,
                                            int termDays, int annualRateBasisPoints, String sourceId) {
        if (server == null || day < 0L || !FinancialCrisisSavedData.get(server).active()
                || !CentralBankFacilityEconomics.validTerms(amountMinor, annualRateBasisPoints, termDays)
                || sourceId == null || sourceId.isBlank()) return false;
        CentralBankFacilitySavedData facilities = CentralBankFacilitySavedData.get(server);
        if (facilities.find(sourceId) != null) return true;
        GovernmentPolicySavedData government = GovernmentPolicySavedData.get(server);
        String spendingId = "central-bank-facility:" + sourceId;
        if (!government.hasSpending(spendingId)
                && !government.spend("central-bank-facility", day, amountMinor, spendingId)) return false;
        return facilities.add(new CentralBankFacilitySavedData.Facility(sourceId, day, amountMinor,
                amountMinor, annualRateBasisPoints, termDays, termDays, -1L));
    }
}
