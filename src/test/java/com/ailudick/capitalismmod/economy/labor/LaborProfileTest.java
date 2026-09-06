package com.ailudick.capitalismmod.economy.labor;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaborProfileTest {
    @Test
    void healthAndEducationChangeHiringReservationWithoutCompounding() {
        EnumMap<LaborSkill, Integer> skills = new EnumMap<>(LaborSkill.class);
        skills.put(LaborSkill.FOUNDATION, 45);
        LaborProfile baseline = new LaborProfile("npc-1", skills, 100, 100L);
        LaborProfile educated = baseline.withHealthAndEducation(100, 100);

        assertEquals(100L, baseline.reservationWageMinor());
        assertEquals(90L, baseline.effectiveReservationWageMinor());
        assertTrue(educated.effectiveReservationWageMinor() > baseline.effectiveReservationWageMinor());
        assertEquals(100L, educated.reservationWageMinor());
        assertEquals(100L, educated.withHealthAndEducation(100, 100).reservationWageMinor());
    }
}
