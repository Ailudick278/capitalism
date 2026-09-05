package com.ailudick.capitalismmod.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerpetualCalendarTest {
    @Test
    void mapsMinecraftDaysToStableGregorianDates() {
        assertEquals(LocalDate.of(2000, 1, 1), PerpetualCalendar.dateAtMinecraftDay(0));
        assertEquals(LocalDate.of(2000, 1, 2), PerpetualCalendar.dateAtMinecraftDay(1));
        assertEquals(LocalDate.of(1999, 12, 31), PerpetualCalendar.dateAtMinecraftDay(-1));
    }

    @Test
    void convertsTicksToMinecraftClockTime() {
        assertEquals("06:00:00", PerpetualCalendar.timeAtMinecraftTicks(0).format());
        assertEquals("07:00:00", PerpetualCalendar.timeAtMinecraftTicks(1_000).format());
        assertEquals("00:00:00", PerpetualCalendar.timeAtMinecraftTicks(18_000).format());
    }
}
