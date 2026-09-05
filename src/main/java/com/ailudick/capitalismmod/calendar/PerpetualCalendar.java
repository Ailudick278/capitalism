package com.ailudick.capitalismmod.calendar;

import net.minecraft.server.level.ServerLevel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Gregorian perpetual calendar used as the common date layer for future systems.
 * Minecraft day 0 is mapped to 2000-01-01; changing this mapping would change all
 * persisted economic dates, so it is deliberately kept stable.
 */
public final class PerpetualCalendar {
    public static final LocalDate MINECRAFT_EPOCH = LocalDate.of(2000, 1, 1);
    public static final long TICKS_PER_DAY = 24_000L;
    private static final DateTimeFormatter CHINESE_DATE = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    private PerpetualCalendar() {
    }

    public record CalendarDay(LocalDate date, DayOfWeek dayOfWeek) {
    }

    /** Future holiday, tax deadline, event and season systems can share this record. */
    public record CalendarEvent(LocalDate date, String id, String title) {
    }

    /** In-game clock value converted from Minecraft ticks. */
    public record TimeOfDay(int hour, int minute, int second, long tickOfDay) {
        public String format() {
            return String.format(Locale.ROOT, "%02d:%02d:%02d", hour, minute, second);
        }
    }

    public static LocalDate dateAtMinecraftDay(long minecraftDay) {
        return MINECRAFT_EPOCH.plusDays(minecraftDay);
    }

    public static LocalDate dateAt(ServerLevel level) {
        return dateAtMinecraftDay(Math.floorDiv(level.getDayTime(), TICKS_PER_DAY));
    }

    public static TimeOfDay timeAtMinecraftTicks(long ticks) {
        long tickOfDay = Math.floorMod(ticks, TICKS_PER_DAY);
        int vanillaHour = (int) (tickOfDay / 1_000L);
        int hour = (vanillaHour + 6) % 24;
        long ticksInHour = tickOfDay % 1_000L;
        int minute = (int) (ticksInHour * 60L / 1_000L);
        int second = (int) ((ticksInHour * 60L % 1_000L) * 60L / 1_000L);
        return new TimeOfDay(hour, minute, second, tickOfDay);
    }

    /** Formats a persisted Minecraft game-time value with the shared calendar. */
    public static String formatMinecraftTicks(long ticks) {
        if (ticks < 0L) return "未知时间";
        long minecraftDay = Math.floorDiv(ticks, TICKS_PER_DAY);
        return format(dateAtMinecraftDay(minecraftDay), timeAtMinecraftTicks(ticks));
    }

    public static TimeOfDay timeAt(ServerLevel level) {
        return timeAtMinecraftTicks(level.getDayTime());
    }

    public static long minecraftDay(LocalDate date) {
        return ChronoUnit.DAYS.between(MINECRAFT_EPOCH, date);
    }

    public static boolean isLeapYear(int year) {
        return java.time.Year.isLeap(year);
    }

    public static int daysInMonth(int year, int month) {
        return YearMonth.of(year, month).lengthOfMonth();
    }

    public static List<CalendarDay> month(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return yearMonth.atDay(1).datesUntil(yearMonth.plusMonths(1).atDay(1))
                .map(date -> new CalendarDay(date, date.getDayOfWeek()))
                .toList();
    }

    /** Reserved extension point; future modules can provide holidays and economic events here. */
    public static List<CalendarEvent> eventsOn(LocalDate date) {
        return List.of();
    }

    public static String format(LocalDate date) {
        return CHINESE_DATE.format(date) + " " + weekdayName(date.getDayOfWeek());
    }

    public static String format(LocalDate date, TimeOfDay time) {
        return format(date) + " " + time.format();
    }

    public static String weekdayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }
}
