package com.ailudick.capitalismmod.economy.contract;

import com.ailudick.capitalismmod.economy.expansion.EconomicActorRef;

/** Pure structural and lifecycle checks for the shared contract index. */
public final class ContractAuditRules {
    private ContractAuditRules() {}

    public static boolean valid(String id, ContractType type, EconomicActorRef proposer,
                                EconomicActorRef counterparty, long createdAt, long startsAt, long endsAt,
                                long agreedAmountMinor, String currencyId, ContractStatus status,
                                long fulfilledQuantity, long agreedQuantity, long breachAmountMinor,
                                boolean currencyExists) {
        if (id == null || id.isBlank() || type == null || !validActor(proposer) || !validActor(counterparty)
                || createdAt < 0L || startsAt < createdAt || endsAt < startsAt
                || agreedAmountMinor < 0L || currencyId == null || currencyId.isBlank() || !currencyExists
                || status == null || fulfilledQuantity < 0L || agreedQuantity < 0L || breachAmountMinor < 0L) {
            return false;
        }
        if (agreedQuantity > 0L && fulfilledQuantity > agreedQuantity) return false;
        if (status == ContractStatus.COMPLETED && agreedQuantity > 0L && fulfilledQuantity < agreedQuantity) {
            return false;
        }
        return status != ContractStatus.BREACHED || breachAmountMinor > 0L;
    }

    private static boolean validActor(EconomicActorRef actor) {
        return actor != null && actor.type() != null && !actor.type().isBlank()
                && actor.id() != null && !actor.id().isBlank();
    }
}
