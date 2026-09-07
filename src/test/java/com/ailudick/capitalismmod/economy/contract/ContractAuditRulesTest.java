package com.ailudick.capitalismmod.economy.contract;

import com.ailudick.capitalismmod.economy.expansion.EconomicActorRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractAuditRulesTest {
    private static final EconomicActorRef SELLER = EconomicActorRef.of("company", "seller");
    private static final EconomicActorRef BUYER = EconomicActorRef.of("company", "buyer");

    @Test
    void acceptsConsistentActiveContract() {
        assertTrue(ContractAuditRules.valid("supply-1", ContractType.SUPPLY, SELLER, BUYER,
                1L, 1L, 100L, 500L, "usd", ContractStatus.ACTIVE,
                3L, 10L, 0L, true));
    }

    @Test
    void rejectsBrokenTimelineCurrencyAndCompletion() {
        assertFalse(ContractAuditRules.valid("supply-1", ContractType.SUPPLY, SELLER, BUYER,
                10L, 1L, 100L, 500L, "usd", ContractStatus.ACTIVE,
                0L, 10L, 0L, true));
        assertFalse(ContractAuditRules.valid("supply-1", ContractType.SUPPLY, SELLER, BUYER,
                1L, 1L, 100L, 500L, "unknown", ContractStatus.ACTIVE,
                0L, 10L, 0L, false));
        assertFalse(ContractAuditRules.valid("supply-1", ContractType.SUPPLY, SELLER, BUYER,
                1L, 1L, 100L, 500L, "usd", ContractStatus.COMPLETED,
                3L, 10L, 0L, true));
    }

    @Test
    void breachedContractMustExposePositiveAmount() {
        assertFalse(ContractAuditRules.valid("supply-1", ContractType.SUPPLY, SELLER, BUYER,
                1L, 1L, 100L, 500L, "usd", ContractStatus.BREACHED,
                0L, 10L, 0L, true));
        assertTrue(ContractAuditRules.valid("supply-1", ContractType.SUPPLY, SELLER, BUYER,
                1L, 1L, 100L, 500L, "usd", ContractStatus.BREACHED,
                0L, 10L, 1L, true));
    }
}
