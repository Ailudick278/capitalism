package com.ailudick.capitalismmod.bank;

/** Pure invariants for the bank equity and loss-provision ledger. */
public final class BankCapitalAuditRules {
    private BankCapitalAuditRules() {
    }

    public static boolean validState(boolean initialized, long capitalMinor, long profitLossMinor,
                                     long lossProvisionMinor, long lastSettlementDay) {
        if (capitalMinor == Long.MIN_VALUE || profitLossMinor == Long.MIN_VALUE
                || lossProvisionMinor < 0L || lastSettlementDay < -1L) return false;
        return initialized || (capitalMinor == 0L && profitLossMinor == 0L
                && lossProvisionMinor == 0L && lastSettlementDay == -1L);
    }

    /** Before today's bank close, the previous provision remains a valid transient state. */
    public static boolean provisionMatchesAfterClose(long provisionMinor, long overdueDebtMinor,
                                                     long lastSettlementDay, long currentDay) {
        if (provisionMinor < 0L || overdueDebtMinor < 0L) return false;
        if (lastSettlementDay < currentDay) return true;
        return provisionMinor == BankCapitalEconomics.lossProvisionTarget(overdueDebtMinor);
    }
}
