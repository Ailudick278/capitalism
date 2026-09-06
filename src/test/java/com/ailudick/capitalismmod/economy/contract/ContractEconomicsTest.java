package com.ailudick.capitalismmod.economy.contract;

import org.junit.jupiter.api.Test;

import com.ailudick.capitalismmod.economy.expansion.EconomicActorRef;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractEconomicsTest {
    @Test
    void lifecycleAllowsForwardFulfillmentAndBreach() {
        assertTrue(ContractEconomics.canTransition(ContractStatus.DRAFT, ContractStatus.OFFERED));
        assertTrue(ContractEconomics.canTransition(ContractStatus.OFFERED, ContractStatus.ACTIVE));
        assertTrue(ContractEconomics.canTransition(ContractStatus.ACTIVE, ContractStatus.COMPLETED));
        assertTrue(ContractEconomics.canTransition(ContractStatus.ACTIVE, ContractStatus.BREACHED));
    }

    @Test
    void terminalContractsCannotReopen() {
        assertFalse(ContractEconomics.canTransition(ContractStatus.COMPLETED, ContractStatus.ACTIVE));
        assertFalse(ContractEconomics.canTransition(ContractStatus.BREACHED, ContractStatus.COMPLETED));
        assertFalse(ContractEconomics.canTransition(ContractStatus.ACTIVE, ContractStatus.OFFERED));
    }

    @Test
    void everyTerminalStateRejectsEveryTransition() {
        for (ContractStatus terminal : new ContractStatus[]{
                ContractStatus.COMPLETED, ContractStatus.CANCELLED,
                ContractStatus.BREACHED, ContractStatus.EXPIRED}) {
            for (ContractStatus next : ContractStatus.values()) {
                assertFalse(ContractEconomics.canTransition(terminal, next),
                        () -> terminal + " must not transition to " + next);
            }
        }
    }

    @Test
    void sameStateAndNullTransitionsAreRejected() {
        for (ContractStatus status : ContractStatus.values()) {
            assertFalse(ContractEconomics.canTransition(status, status));
            assertFalse(ContractEconomics.canTransition(status, null));
            assertFalse(ContractEconomics.canTransition(null, status));
        }
    }

    @Test
    void contractStatusMutationCannotBypassLifecycleRules() {
        EconomicContract offered = new EconomicContract("supply-1", ContractType.SUPPLY,
                EconomicActorRef.of("company", "seller"), EconomicActorRef.of("company", "buyer"),
                1L, 1L, 100L, 500L, "usd", ContractStatus.OFFERED, 0L, 10L, 0L);

        assertSame(offered, offered.withStatus(ContractStatus.COMPLETED));
        EconomicContract active = offered.withStatus(ContractStatus.ACTIVE);
        assertTrue(active.status() == ContractStatus.ACTIVE);
        assertSame(active, active.withStatus(ContractStatus.ACTIVE));
        assertSame(active, active.withStatus(null));
    }
}
