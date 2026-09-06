package com.ailudick.capitalismmod.economy.expansion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EconomicEventServiceTest {
    @Test
    void eventTypeEncodesDirectionWithoutFloatingPoint() {
        EconomicEvent event = new EconomicEvent("shock-1", ExpansionSystem.ECONOMIC_EVENTS,
                "commodity_price_shock_down", 10L, 10L, 100L, null,
                EconomicActorRef.of("commodity", "minecraft:wheat"), 2500L, "bps", "active");
        assertEquals(true, event.activeAt(50L));
        assertEquals(false, event.activeAt(100L));
    }

    @Test
    void automaticCorrectionOnlyActivatesAtExtremeDeviations() {
        assertEquals(-1000, EconomicEventService.automaticCorrectionShockBps(250, 100));
        assertEquals(1000, EconomicEventService.automaticCorrectionShockBps(40, 100));
        assertEquals(0, EconomicEventService.automaticCorrectionShockBps(150, 100));
    }

    @Test
    void logisticsShockChangesCapacityAndTransitTimeWithinBounds() {
        assertEquals(50, EconomicEventService.applyCapacityShock(100, -5000));
        assertEquals(150, EconomicEventService.applyCapacityShock(100, 5000));
        assertEquals(125, EconomicEventService.applyTravelShock(100, -5000));
        assertEquals(75, EconomicEventService.applyTravelShock(100, 5000));
        assertEquals(10, EconomicEventService.applyCapacityShock(100, -9000));
        assertEquals(0.15D, EconomicEventService.applyLogisticsRiskShock(0.1D, -5000), 0.000001D);
        assertEquals(0.05D, EconomicEventService.applyLogisticsRiskShock(0.1D, 5000), 0.000001D);
        assertEquals(0.95D, EconomicEventService.applyLogisticsRiskShock(0.9D, -9000), 0.000001D);
        assertEquals(50, EconomicEventService.effectiveVacancies(100, -5000));
        assertEquals(100, EconomicEventService.effectiveVacancies(100, 5000));
        assertEquals(10, EconomicEventService.effectiveVacancies(100, -9000));
    }
}
