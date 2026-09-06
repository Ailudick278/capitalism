package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.government.GovernmentPolicySavedData;
import com.ailudick.capitalismmod.bank.BankCapitalService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
        var regionalAmount = Commands.argument("percent", IntegerArgumentType.integer(0, 30))
                .executes(c -> regional(c.getSource(), IntegerArgumentType.getInteger(c, "percent")));
        var regional = Commands.literal("regionalSupport").then(regionalAmount);
        var rateAmount = Commands.argument("basisPoints", IntegerArgumentType.integer(-10000, 20000))
                .executes(c -> rate(c.getSource(), IntegerArgumentType.getInteger(c, "basisPoints")));
        var rate = Commands.literal("rate").then(rateAmount);
        var autoInflation = Commands.argument("enabled", BoolArgumentType.bool())
                .executes(c -> autoInflation(c.getSource(), BoolArgumentType.getBool(c, "enabled")));
        var inflationTarget = Commands.argument("indexBps", IntegerArgumentType.integer(9000, 12000))
                .executes(c -> inflationTarget(c.getSource(), IntegerArgumentType.getInteger(c, "indexBps")));
        var inflation = Commands.literal("inflation").then(autoInflation).then(Commands.literal("target").then(inflationTarget));
        root.then(Commands.literal("policy").requires(s -> s.hasPermission(2)).then(benefit).then(rate).then(regional).then(inflation));
        var depositAmount = Commands.argument("amountMinor", IntegerArgumentType.integer(1, 2000000000))
                .executes(c -> deposit(c.getSource(), IntegerArgumentType.getInteger(c, "amountMinor")));
        var deposit = Commands.literal("deposit").then(depositAmount);
        root.then(Commands.literal("treasury").requires(s -> s.hasPermission(2)).then(deposit));
        var recapAmount = Commands.argument("amountMinor", LongArgumentType.longArg(1, 9_000_000_000_000_000L))
                .executes(c -> recapitalize(c.getSource(), LongArgumentType.getLong(c, "amountMinor")));
        root.then(Commands.literal("bank").requires(s -> s.hasPermission(2))
                .then(Commands.literal("recapitalize").then(recapAmount)));
        dispatcher.register(root);
    }

    private static int info(CommandSourceStack source) {
        GovernmentPolicySavedData data = GovernmentPolicySavedData.get(source.getServer());
        source.sendSuccess(() -> Component.literal("government treasuryMinor=" + data.treasuryMinor()
                + " dailyBenefitMinor=" + data.dailyBenefitMinor()
                + " regionalSupportRate=" + data.regionalSupportRatePercent() + "%"
                + " policyRateBps=" + data.policyRateBasisPoints()
                + " autoInflation=" + data.automaticInflationPolicy()
                + " inflationTargetIndexBps=" + data.inflationTargetIndexBps()
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

    private static int rate(CommandSourceStack source, int basisPoints) {
        GovernmentPolicySavedData data = GovernmentPolicySavedData.get(source.getServer());
        if (!data.setPolicyRateBasisPoints(basisPoints)) return 0;
        source.sendSuccess(() -> Component.literal("government policy rate set to " + basisPoints + " basis points"), true);
        return 1;
    }

    private static int autoInflation(CommandSourceStack source, boolean enabled) {
        GovernmentPolicySavedData data = GovernmentPolicySavedData.get(source.getServer());
        data.setAutomaticInflationPolicy(enabled);
        source.sendSuccess(() -> Component.literal("automatic inflation policy set to " + enabled), true);
        return 1;
    }

    private static int inflationTarget(CommandSourceStack source, int target) {
        GovernmentPolicySavedData data = GovernmentPolicySavedData.get(source.getServer());
        if (!data.setInflationTargetIndexBps(target)) return 0;
        source.sendSuccess(() -> Component.literal("inflation target index set to " + target), true);
        return 1;
    }

    private static int regional(CommandSourceStack source, int percent) {
        GovernmentPolicySavedData data = GovernmentPolicySavedData.get(source.getServer());
        if (!data.setRegionalSupportRatePercent(percent)) return 0;
        source.sendSuccess(() -> Component.literal("government regional support rate set to " + percent + "%"), true);
        return 1;
    }

    private static int deposit(CommandSourceStack source, int amount) {
        GovernmentPolicySavedData data = GovernmentPolicySavedData.get(source.getServer());
        if (!data.deposit(amount)) return 0;
        source.sendSuccess(() -> Component.literal("government treasury deposited " + amount + " minor units"), true);
        return 1;
    }

    private static int recapitalize(CommandSourceStack source, long amount) {
        String sourceId = "manual:" + source.getServer().overworld().getGameTime() + ":" + amount;
        long day = source.getServer().overworld().getGameTime()
                / com.ailudick.capitalismmod.calendar.PerpetualCalendar.TICKS_PER_DAY;
        if (!BankCapitalService.injectFromTreasury(source.getServer(), day, amount, sourceId)) {
            source.sendFailure(Component.literal("政府财政余额不足，或银行资本补充失败。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("银行资本已补充 " + amount + " minor units。"), true);
        return 1;
    }
}
