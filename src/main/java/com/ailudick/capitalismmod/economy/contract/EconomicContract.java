package com.ailudick.capitalismmod.economy.contract;

import com.ailudick.capitalismmod.economy.expansion.EconomicActorRef;

import java.util.Objects;

/** Generic contract envelope; domain systems own the detailed terms and settlement. */
public record EconomicContract(String id, ContractType type, EconomicActorRef proposer,
                               EconomicActorRef counterparty, long createdAt, long startsAt,
                               long endsAt, long agreedAmountMinor, String currencyId,
                               ContractStatus status, long fulfilledQuantity, long breachAmountMinor) {
    public EconomicContract {
        id = Objects.requireNonNullElse(id, "").trim();
        currencyId = Objects.requireNonNullElse(currencyId, "").trim();
        if (id.isEmpty() || type == null || proposer == null || counterparty == null
                || createdAt < 0L || startsAt < createdAt || endsAt < startsAt
                || agreedAmountMinor < 0L || fulfilledQuantity < 0L || breachAmountMinor < 0L) {
            throw new IllegalArgumentException("Invalid economic contract");
        }
        status = status == null ? ContractStatus.DRAFT : status;
    }

    public EconomicContract withStatus(ContractStatus next) {
        return new EconomicContract(id, type, proposer, counterparty, createdAt, startsAt, endsAt,
                agreedAmountMinor, currencyId, next, fulfilledQuantity, breachAmountMinor);
    }

    public EconomicContract fulfill(long quantity) {
        if (quantity <= 0L || status != ContractStatus.ACTIVE) return this;
        long next = safeAdd(fulfilledQuantity, quantity);
        return new EconomicContract(id, type, proposer, counterparty, createdAt, startsAt, endsAt,
                agreedAmountMinor, currencyId, status, next, breachAmountMinor);
    }

    public EconomicContract complete() {
        if (status != ContractStatus.ACTIVE) return this;
        return withStatus(ContractStatus.COMPLETED);
    }

    public EconomicContract breach(long amountMinor) {
        if (status != ContractStatus.ACTIVE && status != ContractStatus.OFFERED) return this;
        return new EconomicContract(id, type, proposer, counterparty, createdAt, startsAt, endsAt,
                agreedAmountMinor, currencyId, ContractStatus.BREACHED, fulfilledQuantity,
                safeAdd(breachAmountMinor, Math.max(0L, amountMinor)));
    }

    private static long safeAdd(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }
}
