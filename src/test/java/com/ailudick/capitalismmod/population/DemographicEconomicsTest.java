package com.ailudick.capitalismmod.population;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemographicEconomicsTest {
    @Test
    void ratesRespondToHouseholdConditions() {
        assertTrue(DemographicEconomics.annualBirthRatePerThousand(80)
                > DemographicEconomics.annualBirthRatePerThousand(20));
        assertTrue(DemographicEconomics.annualDeathRatePerThousand(90)
                < DemographicEconomics.annualDeathRatePerThousand(30));
        assertEquals(7, DemographicEconomics.annualDeathRatePerThousand(100));
        assertTrue(DemographicEconomics.annualBirthRatePerThousand(80, 30)
                > DemographicEconomics.annualBirthRatePerThousand(80, 70));
        assertTrue(DemographicEconomics.annualDeathRatePerThousand(70, 80)
                > DemographicEconomics.annualDeathRatePerThousand(70, 30));
    }
}
