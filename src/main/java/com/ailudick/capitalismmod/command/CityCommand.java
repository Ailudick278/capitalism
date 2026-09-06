package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import com.ailudick.capitalismmod.population.PopulationSavedData;
import com.ailudick.capitalismmod.population.CityHousingSavedData;
import com.ailudick.capitalismmod.population.HousingLeaseSavedData;
import com.ailudick.capitalismmod.population.PrivateLandlordSavedData;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.government.PublicConstructionEconomics;
import com.ailudick.capitalismmod.government.PublicConstructionSavedData;
import com.ailudick.capitalismmod.government.CityStatisticsSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
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
        var history = Commands.literal("history").requires(source -> source.hasPermission(2))
                .then(Commands.argument("region", StringArgumentType.word())
                        .executes(c -> history(c.getSource(), StringArgumentType.getString(c, "region"), 7))
                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 365))
                                .executes(c -> history(c.getSource(), StringArgumentType.getString(c, "region"),
                                        IntegerArgumentType.getInteger(c, "days")))));
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
        var projectCount = Commands.argument("count", IntegerArgumentType.integer(1, 1000000))
                .executes(c -> startProject(c.getSource(), StringArgumentType.getString(c, "region"),
                        StringArgumentType.getString(c, "type"), IntegerArgumentType.getInteger(c, "count"), ""))
                .then(Commands.argument("contractor", StringArgumentType.word())
                        .executes(c -> startProject(c.getSource(), StringArgumentType.getString(c, "region"),
                                StringArgumentType.getString(c, "type"), IntegerArgumentType.getInteger(c, "count"),
                                StringArgumentType.getString(c, "contractor"))));
        var projectStart = Commands.literal("start")
                .then(Commands.argument("region", StringArgumentType.word())
                        .then(Commands.argument("type", StringArgumentType.word()).then(projectCount)));
        var projectBid = Commands.literal("bid")
                .then(Commands.argument("projectId", StringArgumentType.word())
                        .then(Commands.argument("companyId", StringArgumentType.word())
                                .then(Commands.argument("unitPriceMinor", LongArgumentType.longArg(1))
                                        .executes(c -> submitBid(c.getSource(), StringArgumentType.getString(c, "projectId"),
                                                StringArgumentType.getString(c, "companyId"),
                                                LongArgumentType.getLong(c, "unitPriceMinor"))))));
        var project = Commands.literal("project").requires(source -> source.hasPermission(2))
                .then(projectStart).then(projectBid);
        var rentAmount = Commands.argument("dailyRentMinor", IntegerArgumentType.integer(0, 1000000000))
                .executes(c -> setRent(c.getSource(), StringArgumentType.getString(c, "region"),
                        IntegerArgumentType.getInteger(c, "dailyRentMinor")));
        var rentRegion = Commands.argument("region", StringArgumentType.word()).then(rentAmount);
        var rentSet = Commands.literal("set").then(rentRegion);
        var landlordRoute = Commands.literal("landlord").then(Commands.argument("region", StringArgumentType.word())
                .then(Commands.argument("landlordId", StringArgumentType.word())
                        .executes(c -> setLandlord(c.getSource(), StringArgumentType.getString(c, "region"),
                                StringArgumentType.getString(c, "landlordId")))));
        var rent = Commands.literal("rent").requires(source -> source.hasPermission(2)).then(rentSet).then(landlordRoute);
        var landlordWithdraw = Commands.literal("withdraw")
                .then(Commands.argument("amountMinor", LongArgumentType.longArg(1))
                        .executes(c -> withdrawLandlord(c.getSource(), LongArgumentType.getLong(c, "amountMinor"))));
        var landlord = Commands.literal("landlord").then(landlordWithdraw);
        var terminateHousing = Commands.literal("terminate").then(Commands.argument("household", StringArgumentType.word())
                .executes(c -> terminateHousing(c.getSource(), StringArgumentType.getString(c, "household"))));
        var housing = Commands.literal("housing").requires(source -> source.hasPermission(2)).then(terminateHousing);
        dispatcher.register(Commands.literal("city").then(info).then(history).then(facility).then(project)
                .then(rent).then(housing).then(landlord));
    }

    private static int info(CommandSourceStack source, String region) {
        LogisticsInfrastructureSavedData infrastructure = LogisticsInfrastructureSavedData.get(source.getServer());
        int residents = PopulationSavedData.get(source.getServer()).population(region);
        int score = infrastructure.publicServiceScore(source.getServer(), region, residents);
        long rent = CityHousingSavedData.get(source.getServer()).dailyRent(region, residents,
                infrastructure.count(region, "housing"));
        HousingLeaseSavedData leases = HousingLeaseSavedData.get(source.getServer());
        long arrears = leases.leases().stream().filter(l -> l.region().equals(region))
                .mapToLong(HousingLeaseSavedData.Lease::arrearsMinor).reduce(0L, CityCommand::add);
        long deposits = leases.leases().stream().filter(l -> l.region().equals(region))
                .mapToLong(HousingLeaseSavedData.Lease::depositHeldMinor).reduce(0L, CityCommand::add);
        int activeLeases = (int) leases.leases().stream().filter(l -> l.region().equals(region)).count();
        long activeProjects = PublicConstructionSavedData.get(source.getServer()).projects().stream()
                .filter(p -> p.region().equals(region) && p.completedUnits() < p.units()).count();
        source.sendSuccess(() -> Component.literal("city region=" + region + " residents=" + residents
                + " housing=" + infrastructure.count(region, "housing") + " school="
                + infrastructure.count(region, "school") + " clinic=" + infrastructure.count(region, "clinic")
                + " serviceScore=" + score + " dailyRentPerResident=" + rent
                + " landlord=" + CityHousingSavedData.get(source.getServer()).landlord(region)
                + " activeLeases=" + activeLeases + " rentArrearsMinor=" + arrears
                + " depositsHeldMinor=" + deposits + " activeConstructionProjects=" + activeProjects), false);
        return score;
    }

    private static int history(CommandSourceStack source, String region, int days) {
        var entries = CityStatisticsSavedData.get(source.getServer()).snapshots(region, days);
        for (var entry : entries) {
            source.sendSuccess(() -> Component.literal("city history day=" + entry.day()
                    + " residents=" + entry.residents() + " serviceScore=" + entry.serviceScore()
                    + " unemploymentRate=" + entry.unemploymentRate() + "% dailyRentPerResident="
                    + entry.dailyRentPerResident() + " treasuryMinor=" + entry.treasuryMinor()
                    + " activeProjects=" + entry.activeProjects()), false);
        }
        return entries.size();
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

    private static int startProject(CommandSourceStack source, String region, String type, int count, String contractor) {
        if (!PublicConstructionEconomics.validFacility(type)) {
            source.sendFailure(Component.literal("Invalid construction type."));
            return 0;
        }
        PublicConstructionSavedData data = PublicConstructionSavedData.get(source.getServer());
        long day = source.getServer().overworld().getGameTime() / 24000L;
        String id = "city-project:" + day + ":" + region + ":" + type + ":" + data.projects().size();
        if (!contractor.isBlank()) {
            var company = CompanySavedData.get(source.getServer()).get(contractor);
            if (company == null || !"construction".equals(company.type())) {
                source.sendFailure(Component.literal("Contractor must be an existing construction company."));
                return 0;
            }
        }
        if (data.start(id, region, type, count, contractor, day) == null) {
            source.sendFailure(Component.literal("Unable to create construction project."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("city project started id=" + id + " region=" + region
                + " type=" + type + " units=" + count + " unitCostMinor="
                + PublicConstructionEconomics.unitCost(type) + " contractor="
                + (contractor.isBlank() ? "government-direct" : contractor)), true);
        return count;
    }

    private static int submitBid(CommandSourceStack source, String projectId, String companyId, long price) {
        var company = CompanySavedData.get(source.getServer()).get(companyId);
        if (company == null || !"construction".equals(company.type())) {
            source.sendFailure(Component.literal("Bidder must be an existing construction company."));
            return 0;
        }
        long day = source.getServer().overworld().getGameTime() / 24000L;
        if (!PublicConstructionSavedData.get(source.getServer()).submitBid(projectId, companyId, price, day)) {
            source.sendFailure(Component.literal("Bid rejected: project closed, duplicate, or price is invalid."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("city construction bid submitted project=" + projectId
                + " company=" + companyId + " unitPriceMinor=" + price), true);
        return 1;
    }

    private static int setRent(CommandSourceStack source, String region, int rent) {
        if (!CityHousingSavedData.get(source.getServer()).setBaseRent(region, rent)) {
            source.sendFailure(Component.literal("Invalid rent or region.")); return 0;
        }
        source.sendSuccess(() -> Component.literal("city base daily rent set region=" + region
                + " rentMinor=" + rent), true);
        return 1;
    }

    private static int setLandlord(CommandSourceStack source, String region, String landlordId) {
        boolean playerLandlord = landlordId.startsWith("player:") && validUuid(landlordId.substring("player:".length()));
        if (!"government".equals(landlordId) && !playerLandlord && CompanySavedData.get(source.getServer()).get(landlordId) == null) {
            source.sendFailure(Component.literal("Landlord must be government or an existing company ID.")); return 0;
        }
        if (!CityHousingSavedData.get(source.getServer()).setLandlord(region, landlordId)) {
            source.sendFailure(Component.literal("Invalid region or landlord.")); return 0;
        }
        source.sendSuccess(() -> Component.literal("city housing landlord set region=" + region
                + " landlord=" + landlordId), true);
        return 1;
    }

    private static int withdrawLandlord(CommandSourceStack source, long amount) {
        try {
            if (!PrivateLandlordSavedData.get(source.getServer()).withdraw(source.getPlayerOrException(), amount)) {
                source.sendFailure(Component.literal("Private landlord receivable balance is insufficient.")); return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("Only a player can withdraw private landlord income.")); return 0;
        }
        source.sendSuccess(() -> Component.literal("private landlord income withdrawn minor=" + amount), false);
        return 1;
    }

    private static boolean validUuid(String value) { try { java.util.UUID.fromString(value); return true; } catch (IllegalArgumentException e) { return false; } }

    private static int terminateHousing(CommandSourceStack source, String householdId) {
        PopulationSavedData population = PopulationSavedData.get(source.getServer());
        if (population.find(householdId) == null) {
            source.sendFailure(Component.literal("Household not found.")); return 0;
        }
        HousingLeaseSavedData leases = HousingLeaseSavedData.get(source.getServer());
        HousingLeaseSavedData.Lease previousLease = leases.lease(householdId);
        HousingLeaseSavedData.Termination termination = leases
                .terminate(householdId, source.getServer().overworld().getGameTime() / 24000L, "admin_termination");
        if (termination == null) {
            source.sendFailure(Component.literal("Lease is not eligible for termination.")); return 0;
        }
        if (termination.depositReleasedMinor() > 0L) population.addCash(householdId, termination.depositReleasedMinor());
        if (previousLease != null) leases.rehouse(householdId, previousLease.region(),
                source.getServer().overworld().getGameTime() / 24000L, previousLease.dailyRentMinor());
        source.sendSuccess(() -> Component.literal("housing lease terminated household=" + householdId
                + " depositRefundMinor=" + termination.depositReleasedMinor()
                + " residualArrearsMinor=" + termination.residualArrearsMinor() + " rehoused=true"), true);
        return 1;
    }

    private static long add(long left, long right) {
        try { return Math.addExact(left, right); } catch (ArithmeticException e) { return Long.MAX_VALUE; }
    }
}
