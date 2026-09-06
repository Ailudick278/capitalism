package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.population.PopulationSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Read-only population overview for the first simulation layer. */
public final class PopulationCommand {
    private PopulationCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("population").then(Commands.literal("info")
                .executes(c -> info(c.getSource(), "spawn"))
                .then(Commands.argument("region", StringArgumentType.word())
                        .executes(c -> info(c.getSource(), StringArgumentType.getString(c, "region"))))));
    }
    private static int info(CommandSourceStack source, String region) {
        PopulationSavedData data = PopulationSavedData.get(source.getServer());
        int households = (int) data.households().stream().filter(h -> h.region().equals(region)).count();
        int population = data.population(region);
        long demand = data.dailyDemand(region);
        source.sendSuccess(() -> Component.literal("population region=" + region + " households=" + households + " residents=" + population + " dailyNeedMinor=" + demand), false);
        return households;
    }
}
