package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.government.GovernmentPolicySavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Operator controls for the persistent fiscal-policy prototype. */
public final class GovernmentCommand {
    private GovernmentCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("government");
        root.then(Commands.literal("info").executes(c -> info(c.getSource())));
        var benefitAmount = Commands.argument("dailyMinor", IntegerArgumentType.integer(0, 1000000000))
                .executes(c -> benefit(c.getSource(), IntegerArgumentType.getInteger(c, "dailyMinor")));
        var benefit = Commands.literal("benefit").then(benefitAmount);
        root.then(Commands.literal("policy").requires(s -> s.hasPermission(2)).then(benefit));
        var depositAmount = Commands.argument("amountMinor", IntegerArgumentType.integer(1, 2000000000))
                .executes(c -> deposit(c.getSource(), IntegerArgumentType.getInteger(c, "amountMinor")));
        var deposit = Commands.literal("deposit").then(depositAmount);
        root.then(Commands.literal("treasury").requires(s -> s.hasPermission(2)).then(deposit));
        dispatcher.register(root);
    }

    private static int info(CommandSourceStack source) {
        GovernmentPolicySavedData data = GovernmentPolicySavedData.get(source.getServer());
        source.sendSuccess(() -> Component.literal("government treasuryMinor=" + data.treasuryMinor()
                + " dailyBenefitMinor=" + data.dailyBenefitMinor()
                + " transfers=" + data.transactions().size()
                + " taxRevenues=" + data.taxRevenues().size()), false);
        return 1;
    }

    private static int benefit(CommandSourceStack source, int amount) {
        GovernmentPolicySavedData data = GovernmentPolicySavedData.get(source.getServer());
        if (!data.setDailyBenefit(amount)) return 0;
        source.sendSuccess(() -> Component.literal("government daily benefit set to " + amount + " minor units"), true);
        return 1;
    }

    private static int deposit(CommandSourceStack source, int amount) {
        GovernmentPolicySavedData data = GovernmentPolicySavedData.get(source.getServer());
        if (!data.deposit(amount)) return 0;
        source.sendSuccess(() -> Component.literal("government treasury deposited " + amount + " minor units"), true);
        return 1;
    }
}
