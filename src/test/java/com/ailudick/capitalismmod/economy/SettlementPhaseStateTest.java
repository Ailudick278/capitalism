package com.ailudick.capitalismmod.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementPhaseStateTest {
    @Test
    void completedPhaseCannotBeDowngradedDuringRecovery() {
        assertTrue(SettlementPhaseState.preservesCompleted("completed", "started"));
        assertFalse(SettlementPhaseState.preservesCompleted("started", "completed"));
        assertFalse(SettlementPhaseState.preservesCompleted("completed", "completed"));
    }

    @Test
    void dayCompletesOnlyAfterAllPhasesAreDurable() {
        assertTrue(SettlementPhaseState.isDayComplete(true, true, true));
        assertFalse(SettlementPhaseState.isDayComplete(true, true, false));
    }
}
