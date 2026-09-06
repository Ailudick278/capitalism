package com.ailudick.capitalismmod.population;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EducationHealthEconomicsTest {
    @Test
    void servicesImproveHumanCapitalAndPoorHealthReducesLabor() {
        assertTrue(EducationHealthEconomics.nextEducation(10, 100, 1) > 10);
        assertTrue(EducationHealthEconomics.nextHealth(50, 100, 100) > 50);
        assertTrue(EducationHealthEconomics.nextHealth(50, 0, 0) < 50);
        assertEquals(0, EducationHealthEconomics.laborParticipation(80, 0));
        assertEquals(35, EducationHealthEconomics.laborParticipation(35, 1));
    }
}
