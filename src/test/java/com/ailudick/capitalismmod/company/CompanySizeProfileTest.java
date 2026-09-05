package com.ailudick.capitalismmod.company;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompanySizeProfileTest {
    @Test
    void usesEmployeesAndEitherTurnoverOrAssets() {
        assertEquals(CompanySizeProfile.MICRO,
                CompanySizeProfile.classify(9, 2_000_000L, 5_000_000L));
        assertEquals(CompanySizeProfile.SMALL,
                CompanySizeProfile.classify(10, 10_000_000L, 20_000_000L));
        assertEquals(CompanySizeProfile.MEDIUM,
                CompanySizeProfile.classify(50, 50_000_000L, 50_000_000L));
        assertEquals(CompanySizeProfile.LARGE,
                CompanySizeProfile.classify(250, 1L, 1L));
    }

    @Test
    void invalidInputsAreSafelyClamped() {
        assertEquals(CompanySizeProfile.MICRO, CompanySizeProfile.classify(-1, -1L, -1L));
    }
}
