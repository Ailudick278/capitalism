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
        boolean added = facilities.add(new CentralBankFacilitySavedData.Facility(sourceId, day, amountMinor,
                amountMinor, annualRateBasisPoints, termDays, termDays, -1L));
        if (added) BankLiquiditySavedData.get(server).grantEmergencyLiquidity(amountMinor);
        return added;
    }

    /** Settles one installment per day; each cash leg is protected by its own receipt. */
    public static int settleDaily(MinecraftServer server, long day) {
        if (server == null || day < 0L) return 0;
        CentralBankFacilitySavedData data = CentralBankFacilitySavedData.get(server);
        BankCapitalSavedData capital = BankCapitalSavedData.get(server);
        GovernmentPolicySavedData government = GovernmentPolicySavedData.get(server);
        int settled = 0;
        for (CentralBankFacilitySavedData.Facility facility : data.facilities()) {
            if (facility.daysRemaining() <= 0 || facility.lastSettlementDay() >= day) continue;
            long principal = CentralBankFacilityEconomics.principalInstallment(
                    facility.remainingPrincipal(), facility.daysRemaining());
            long interest = CentralBankFacilityEconomics.interestDue(
                    facility.remainingPrincipal(), facility.annualRateBasisPoints(), 1);
            long payment = principal > Long.MAX_VALUE - interest ? Long.MAX_VALUE : principal + interest;
            String receipt = "central-bank-repayment:" + facility.id() + ":" + day;
            if (!capital.hasTransaction(receipt)) {
                if (payment <= 0L || capital.capitalMinor() < payment
                        || !capital.applyTransactionOnce(receipt, interest, payment)) continue;
            }
            String governmentReceipt = receipt + ":treasury";
            if (!government.hasDeposit(governmentReceipt)
                    && !government.depositOnce(payment, governmentReceipt)) continue;
            BankLiquiditySavedData.get(server).consumeEmergencyLiquidity(principal);
            long remaining = Math.max(0L, facility.remainingPrincipal() - principal);
            int days = Math.max(0, facility.daysRemaining() - 1);
            data.replace(new CentralBankFacilitySavedData.Facility(facility.id(), facility.issuedDay(),
                    facility.principalMinor(), remaining, facility.annualRateBasisPoints(),
                    facility.termDays(), days, day));
            settled++;
        }
        return settled;
    }
}
