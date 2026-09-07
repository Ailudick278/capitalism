package com.ailudick.capitalismmod.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommodityPriceAuditRulesTest {
    @Test
    void validatesPriceAndOhlcBounds() {
        assertTrue(CommodityPriceAuditRules.validPrice("minecraft:wheat", 110L, 100L, 105L));
        assertTrue(CommodityPriceAuditRules.validCandle(100L, 120L, 90L, 110L));
        assertFalse(CommodityPriceAuditRules.validCandle(100L, 95L, 90L, 110L));
    }
}
