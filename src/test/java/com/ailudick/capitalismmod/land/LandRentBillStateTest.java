package com.ailudick.capitalismmod.land;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LandRentBillStateTest {
    @Test
    void acceptsOnlyAuditableBillStates() {
        assertTrue(LandRentBillStatus.isValid("PENDING"));
        assertTrue(LandRentBillStatus.isValid("PAID"));
        assertTrue(LandRentBillStatus.isValid("DEFAULTED"));
        assertFalse(LandRentBillStatus.isValid("UNKNOWN"));
        assertFalse(LandRentBillStatus.isValid(null));
    }
}
