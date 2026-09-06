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
}
