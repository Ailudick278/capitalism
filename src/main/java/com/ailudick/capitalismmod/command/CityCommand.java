package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import com.ailudick.capitalismmod.population.PopulationSavedData;
import com.ailudick.capitalismmod.population.CityHousingSavedData;
import com.ailudick.capitalismmod.population.HousingLeaseSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Administrative entry point for the first persisted public-service layer. */
public final class CityCommand {
    private CityCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var info = Commands.literal("info")
                .executes(c -> info(c.getSource(), "spawn"))
                .then(Commands.argument("region", StringArgumentType.word())
                        .executes(c -> info(c.getSource(), StringArgumentType.getString(c, "region"))));
        var add = Commands.literal("add").then(Commands.argument("region", StringArgumentType.word())
                .then(Commands.argument("type", StringArgumentType.word())
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 1000000))
                                .executes(c -> change(c.getSource(), StringArgumentType.getString(c, "region"),
                                        StringArgumentType.getString(c, "type"), IntegerArgumentType.getInteger(c, "count"))))));
        var remove = Commands.literal("remove").then(Commands.argument("region", StringArgumentType.word())
                .then(Commands.argument("type", StringArgumentType.word())
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 1000000))
                                .executes(c -> change(c.getSource(), StringArgumentType.getString(c, "region"),
                                        StringArgumentType.getString(c, "type"), -IntegerArgumentType.getInteger(c, "count"))))));
        var facility = Commands.literal("facility").requires(source -> source.hasPermission(2))
                .then(add).then(remove);
        var rentAmount = Commands.argument("dailyRentMinor", IntegerArgumentType.integer(0, 1000000000))
                .executes(c -> setRent(c.getSource(), StringArgumentType.getString(c, "region"),
                        IntegerArgumentType.getInteger(c, "dailyRentMinor")));
        var rentRegion = Commands.argument("region", StringArgumentType.word()).then(rentAmount);
        var rentSet = Commands.literal("set").then(rentRegion);
        var rent = Commands.literal("rent").requires(source -> source.hasPermission(2)).then(rentSet);
        var terminateHousing = Commands.literal("terminate").then(Commands.argument("household", StringArgumentType.word())
                .executes(c -> terminateHousing(c.getSource(), StringArgumentType.getString(c, "household"))));
        var housing = Commands.literal("housing").requires(source -> source.hasPermission(2)).then(terminateHousing);
        dispatcher.register(Commands.literal("city").then(info).then(facility).then(rent).then(housing));
    }

    private static int info(CommandSourceStack source, String region) {
        LogisticsInfrastructureSavedData infrastructure = LogisticsInfrastructureSavedData.get(source.getServer());
        int residents = PopulationSavedData.get(source.getServer()).population(region);
        int score = infrastructure.publicServiceScore(region, residents);
        long rent = CityHousingSavedData.get(source.getServer()).dailyRent(region, residents,
                infrastructure.count(region, "housing"));
        source.sendSuccess(() -> Component.literal("city region=" + region + " residents=" + residents
                + " housing=" + infrastructure.count(region, "housing") + " school="
                + infrastructure.count(region, "school") + " clinic=" + infrastructure.count(region, "clinic")
                + " serviceScore=" + score + " dailyRentPerResident=" + rent), false);
        return score;
    }

    private static int change(CommandSourceStack source, String region, String type, int delta) {
        LogisticsInfrastructureSavedData data = LogisticsInfrastructureSavedData.get(source.getServer());
        if (!data.changePublicFacility(region, type, delta)) {
            source.sendFailure(Component.literal("Invalid public facility, count, or region."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("city facility " + (delta > 0 ? "added" : "removed")
                + " type=" + type + " count=" + Math.abs(delta) + " region=" + region), true);
        return Math.abs(delta);
    }

    private static int setRent(CommandSourceStack source, String region, int rent) {
        if (!CityHousingSavedData.get(source.getServer()).setBaseRent(region, rent)) {
            source.sendFailure(Component.literal("Invalid rent or region.")); return 0;
        }
        source.sendSuccess(() -> Component.literal("city base daily rent set region=" + region
                + " rentMinor=" + rent), true);
        return 1;
    }

    private static int terminateHousing(CommandSourceStack source, String householdId) {
        PopulationSavedData population = PopulationSavedData.get(source.getServer());
        if (population.find(householdId) == null) {
            source.sendFailure(Component.literal("Household not found.")); return 0;
        }
        HousingLeaseSavedData.Termination termination = HousingLeaseSavedData.get(source.getServer())
                .terminate(householdId, source.getServer().overworld().getGameTime() / 24000L, "admin_termination");
        if (termination == null) {
            source.sendFailure(Component.literal("Lease is not eligible for termination.")); return 0;
        }
        if (termination.depositReleasedMinor() > 0L) population.addCash(householdId, termination.depositReleasedMinor());
        source.sendSuccess(() -> Component.literal("housing lease terminated household=" + householdId
                + " depositRefundMinor=" + termination.depositReleasedMinor()
                + " residualArrearsMinor=" + termination.residualArrearsMinor()), true);
        return 1;
    }
}
