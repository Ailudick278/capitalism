package com.ailudick.capitalismmod.data;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CapitalismDataIndustryJsonTest {
    @Test
    void positiveIncomeServiceDefaultsToOneWorker() {
        CapitalismData.IndustryJson service = new CapitalismData.IndustryJson(
                "transport", Map.of("minecraft:coal", 1), Map.of(), 45L);
        CapitalismData.IndustryJson finance = new CapitalismData.IndustryJson(
                "finance", Map.of(), Map.of(), 0L);

        assertEquals(1, service.workers_per_cycle);
        assertEquals(0, finance.workers_per_cycle);
    }
}
