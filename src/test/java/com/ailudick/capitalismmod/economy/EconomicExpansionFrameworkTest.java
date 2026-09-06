package com.ailudick.capitalismmod.economy;

import com.ailudick.capitalismmod.economy.expansion.EconomicActorRef;
import com.ailudick.capitalismmod.economy.expansion.EconomicEvent;
import com.ailudick.capitalismmod.economy.expansion.ExpansionSystem;
import com.ailudick.capitalismmod.economy.expansion.ExpansionSystemCatalog;
import com.ailudick.capitalismmod.economy.contract.ContractStatus;
import com.ailudick.capitalismmod.economy.contract.ContractType;
import com.ailudick.capitalismmod.economy.contract.EconomicContract;
import com.ailudick.capitalismmod.economy.labor.LaborProfile;
import com.ailudick.capitalismmod.economy.labor.LaborSkill;
import com.ailudick.capitalismmod.population.Household;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomicExpansionFrameworkTest {
    @Test
    void definesTenDistinctExtensibleSystems() {
        Set<String> ids = java.util.Arrays.stream(ExpansionSystem.values())
                .map(ExpansionSystem::id).collect(Collectors.toSet());
        assertEquals(10, ExpansionSystem.values().length);
        assertEquals(10, ids.size());
        assertEquals(10, ExpansionSystemCatalog.all().size());
        assertTrue(ExpansionSystemCatalog.all().values().stream()
                .allMatch(descriptor -> !descriptor.coreObjects().isEmpty()
                        && !descriptor.outputs().isEmpty()));
    }

    @Test
    void validatesStableActorsAndEventWindows() {
        EconomicActorRef company = EconomicActorRef.of("company", "factory-1");
        EconomicEvent event = new EconomicEvent("event-1", ExpansionSystem.CONTRACTS, "delivery",
                10L, 20L, 40L, company, null, 500L, "usd", "active");

        assertTrue(event.activeAt(20L));
        assertTrue(event.activeAt(39L));
        assertTrue(!event.activeAt(40L));
        assertThrows(IllegalArgumentException.class, () -> new EconomicActorRef("", "id"));
        assertThrows(IllegalArgumentException.class, () -> new EconomicEvent("bad", ExpansionSystem.LABOR,
                "job", 10L, 50L, 40L, null, null, 0L, "usd", "active"));
    }

    @Test
    void modelsContractLifecycleAndPortableLaborSkills() {
        EconomicActorRef company = EconomicActorRef.of("company", "factory-1");
        EconomicActorRef worker = EconomicActorRef.of("household", "worker-1");
        EconomicContract contract = new EconomicContract("contract-1", ContractType.EMPLOYMENT,
                company, worker, 0L, 1L, 100L, 1_000L, "cny", ContractStatus.OFFERED, 0L, 0L)
                .withStatus(ContractStatus.ACTIVE);
        EconomicContract fulfilled = contract.fulfill(1L).complete();
        assertEquals(ContractStatus.COMPLETED, fulfilled.status());
        assertEquals(1L, fulfilled.fulfilledQuantity());

        LaborProfile profile = new LaborProfile("worker-1",
                Map.of(LaborSkill.FOUNDATION, 80, LaborSkill.TECHNICAL, 60), 75, 100L);
        assertEquals(80, profile.skill(LaborSkill.FOUNDATION));
        assertEquals(70, profile.averageSkill());

        Household household = new Household("household-1", "spawn", 3, 2, 500L, 100L, 100, -1L);
        Household settled = household.withSettlement(4L, 200L, 40, "spawn");
        assertEquals(4L, settled.lastSettlementDay());
        assertEquals(40, settled.satisfaction());
    }
}
