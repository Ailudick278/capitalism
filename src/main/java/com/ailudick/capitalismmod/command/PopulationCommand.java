package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.population.PopulationSavedData;
import com.ailudick.capitalismmod.economy.labor.LaborMarketSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Population overview and controlled administrative actions. */
public final class PopulationCommand {
    private PopulationCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("population").then(Commands.literal("info")
                .executes(c -> info(c.getSource(), "spawn"))
                .then(Commands.argument("region", StringArgumentType.word())
                        .executes(c -> info(c.getSource(), StringArgumentType.getString(c, "region")))))
                .then(Commands.literal("seed").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("region", StringArgumentType.word())
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 10000))
                                        .executes(c -> seed(c.getSource(), StringArgumentType.getString(c, "region"), IntegerArgumentType.getInteger(c, "count"))))))
                .then(Commands.literal("merge").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("source", StringArgumentType.word())
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .executes(c -> merge(c.getSource(), StringArgumentType.getString(c, "source"),
                                                StringArgumentType.getString(c, "target")))))));
    }
    private static int info(CommandSourceStack source, String region) {
        PopulationSavedData data = PopulationSavedData.get(source.getServer());
        int households = (int) data.households().stream().filter(h -> h.region().equals(region)).count();
        int population = data.population(region);
        long demand = data.dailyDemand(region);
        long ageTotal = data.households().stream().filter(h -> h.region().equals(region))
                .mapToLong(h -> h.averageAge() * (long) h.size()).sum();
        int averageAge = population <= 0 ? 0 : (int) (ageTotal / population);
        int unemployed = data.households().stream().filter(h -> h.region().equals(region)
                && h.unemploymentDays() > 0).mapToInt(h -> h.size()).sum();
        int children = data.households().stream().filter(h -> h.region().equals(region)).mapToInt(h -> h.children()).sum();
        int elderly = data.households().stream().filter(h -> h.region().equals(region)).mapToInt(h -> h.elderly()).sum();
        int employed = (int) data.households().stream().filter(h -> h.region().equals(region)
                && !LaborMarketSavedData.get(source.getServer()).activeForWorker(h.id()).isEmpty()).count();
        source.sendSuccess(() -> Component.literal("population region=" + region + " households=" + households
                + " residents=" + population + " averageAge=" + averageAge + " employedHouseholds=" + employed
                + " unemployedResidents=" + unemployed + " children=" + children + " elderly=" + elderly
                + " dailyNeedMinor=" + demand), false);
        return households;
    }
    private static int seed(CommandSourceStack source, String region, int count) {
        int created = com.ailudick.capitalismmod.population.PopulationService.seedNpc(source.getServer(), region, count);
        source.sendSuccess(() -> Component.literal("seeded npc households=" + created + " region=" + region), true);
        return created;
    }
    private static int merge(CommandSourceStack source, String sourceId, String targetId) {
        if (!PopulationSavedData.get(source.getServer()).merge(sourceId, targetId)) {
            source.sendFailure(Component.literal("NPC households must be distinct and in the same region."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("merged NPC household " + sourceId + " into " + targetId), true);
        return 1;
    }
}
