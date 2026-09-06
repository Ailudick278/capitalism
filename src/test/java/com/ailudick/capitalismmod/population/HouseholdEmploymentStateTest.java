package com.ailudick.capitalismmod.population;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HouseholdEmploymentStateTest {
    @Test
    void unemploymentDaysAccumulateOnlyForWorkingAgeHouseholds() {
        Household household = new Household("npc-1", "spawn", 1, 1, 0L, 100L, 50,
                -1L, -1L, 70, 10);
        Household unemployed = household.withEmploymentState(false).withEmploymentState(false);
        assertEquals(2, unemployed.unemploymentDays());
        Household contributor = household;
        for (int i = 0; i < 7; i++) contributor = contributor.withEmploymentState(true);
        assertEquals(7, contributor.employmentDays());
        assertEquals(0, unemployed.withEmploymentState(true).unemploymentDays());

        Household child = new Household("npc-2", "spawn", 1, 0, 0L, 100L, 50,
                -1L, -1L, 70, 10).withEmploymentState(false);
        assertEquals(0, child.unemploymentDays());
    }
}
