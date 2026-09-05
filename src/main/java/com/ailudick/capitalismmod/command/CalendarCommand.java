package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.time.LocalDate;

/** In-game access to the shared perpetual calendar layer. */
public final class CalendarCommand {
    private CalendarCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("calendar");
        root.executes(context -> today(context.getSource()));
        root.then(Commands.literal("today").executes(context -> today(context.getSource())));
        root.then(Commands.literal("time").executes(context -> time(context.getSource())));
        root.then(Commands.literal("date")
                .then(Commands.argument("year", IntegerArgumentType.integer(1, 9999))
                        .then(Commands.argument("month", IntegerArgumentType.integer(1, 12))
                                .then(Commands.argument("day", IntegerArgumentType.integer(1, 31))
                                        .executes(context -> date(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "year"),
                                                IntegerArgumentType.getInteger(context, "month"),
                                                IntegerArgumentType.getInteger(context, "day")))))));
        root.then(Commands.literal("month")
                .then(Commands.argument("year", IntegerArgumentType.integer(1, 9999))
                        .then(Commands.argument("month", IntegerArgumentType.integer(1, 12))
                                .executes(context -> month(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "year"),
                                        IntegerArgumentType.getInteger(context, "month"))))));
        dispatcher.register(root);
    }

    private static int today(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        LocalDate date = PerpetualCalendar.dateAt(level);
        source.sendSuccess(() -> Component.literal("游戏日期：" + PerpetualCalendar.format(date, PerpetualCalendar.timeAt(level))
                + " | Minecraft第 " + Math.floorDiv(level.getDayTime(), PerpetualCalendar.TICKS_PER_DAY) + " 天"), false);
        return 1;
    }

    private static int time(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        var time = PerpetualCalendar.timeAt(level);
        source.sendSuccess(() -> Component.literal("游戏时间：" + time.format()
                + " | 当日 tick " + time.tickOfDay()), false);
        return 1;
    }

    private static int date(CommandSourceStack source, int year, int month, int day) {
        try {
            LocalDate date = LocalDate.of(year, month, day);
            source.sendSuccess(() -> Component.literal(PerpetualCalendar.format(date)
                    + " | Minecraft第 " + PerpetualCalendar.minecraftDay(date) + " 天"), false);
            return 1;
        } catch (java.time.DateTimeException exception) {
            source.sendFailure(Component.literal("无效的日期：" + year + "-" + month + "-" + day));
            return 0;
        }
    }

    private static int month(CommandSourceStack source, int year, int month) {
        var days = PerpetualCalendar.month(year, month);
        source.sendSuccess(() -> Component.literal(year + "年" + month + "月，共 " + days.size() + " 天"), false);
        days.forEach(day -> source.sendSuccess(() -> Component.literal(
                day.date().getDayOfMonth() + "日 " + PerpetualCalendar.weekdayName(day.dayOfWeek())), false));
        return days.size();
    }
}
