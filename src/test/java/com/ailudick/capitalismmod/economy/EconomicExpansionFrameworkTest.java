package com.ailudick.capitalismmod.economy;

import com.ailudick.capitalismmod.economy.expansion.EconomicActorRef;
import com.ailudick.capitalismmod.economy.expansion.EconomicEvent;
import com.ailudick.capitalismmod.economy.expansion.ExpansionSystem;
import com.ailudick.capitalismmod.economy.expansion.ExpansionSystemCatalog;
import org.junit.jupiter.api.Test;

import java.util.Set;
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
}
