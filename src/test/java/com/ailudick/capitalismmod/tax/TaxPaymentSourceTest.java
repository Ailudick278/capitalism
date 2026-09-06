package com.ailudick.capitalismmod.tax;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxPaymentSourceTest {
    @Test
    void sourceIsStableAndBoundToThePaymentPosition() {
        String source = TaxPaymentSource.company("bill", 100L, 25L);
        assertEquals(source, TaxPaymentSource.company("bill", 100L, 25L));
        assertNotEquals(source, TaxPaymentSource.company("bill", 125L, 25L));
        assertTrue(TaxPaymentSource.company("", 0L, 25L).isBlank());
    }
}
