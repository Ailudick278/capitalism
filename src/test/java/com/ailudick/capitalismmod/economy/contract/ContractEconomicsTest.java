package com.ailudick.capitalismmod.economy.contract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
