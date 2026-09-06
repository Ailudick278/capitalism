package com.ailudick.capitalismmod.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommodityPriceEconomicsTest {
    @Test
    void excessDemandRaisesAndExcessSupplyLowersPrice() {
        long demandPrice = CommodityPriceEconomics.nextPrice(100L, 100L, 100L, 0L);
        long supplyPrice = CommodityPriceEconomics.nextPrice(100L, 100L, 0L, 100L);
        assertTrue(demandPrice > 100L);
        assertTrue(supplyPrice < 100L);
    }

    @Test
    void priceNeverBecomesNonPositive() {
        assertTrue(CommodityPriceEconomics.nextPrice(1L, 1L, 0L, Long.MAX_VALUE) >= 1L);
    }

    @Test
    void basisPointShocksMovePriceWithoutCrossingZero() {
        assertEquals(110L, CommodityPriceEconomics.applyBasisPointShock(100L, 1000));
        assertEquals(90L, CommodityPriceEconomics.applyBasisPointShock(100L, -1000));
        assertEquals(1L, CommodityPriceEconomics.applyBasisPointShock(1L, -9000));
    }
}
