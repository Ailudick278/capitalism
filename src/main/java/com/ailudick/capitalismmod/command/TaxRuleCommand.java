package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.tax.TaxRule;
import com.ailudick.capitalismmod.tax.TaxRuleSavedData;
import com.ailudick.capitalismmod.tax.TaxRuleService;
import com.ailudick.capitalismmod.tax.TaxType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/** Operator commands for the server tax rule table. Rates are entered as percentages. */
public final class TaxRuleCommand {
    private TaxRuleCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("taxrule").requires(source -> source.hasPermission(2));
        root.then(Commands.literal("list").executes(context -> list(context.getSource())));
        root.then(Commands.literal("history")
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(ids(), builder))
                        .executes(context -> history(context.getSource(), StringArgumentType.getString(context, "type")))));
        root.then(Commands.literal("schedule")
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(ids(), builder))
                        .then(Commands.argument("ratePercent", DoubleArgumentType.doubleArg(0.0, 100.0))
                                .then(Commands.argument("thresholdMinor", LongArgumentType.longArg(0))
                                        .then(Commands.argument("exemptionMinor", LongArgumentType.longArg(0))
                                                .then(Commands.argument("effectiveFrom", LongArgumentType.longArg(0))
                                                        .executes(context -> set(context.getSource(),
                                                                StringArgumentType.getString(context, "type"),
                                                                DoubleArgumentType.getDouble(context, "ratePercent"),
                                                                LongArgumentType.getLong(context, "thresholdMinor"),
                                                                LongArgumentType.getLong(context, "exemptionMinor"),
                                                                LongArgumentType.getLong(context, "effectiveFrom"), true))))))));
        root.then(Commands.literal("reset")
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(ids(), builder))
                        .executes(context -> reset(context.getSource(), StringArgumentType.getString(context, "type")))));
        root.then(Commands.literal("set")
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(ids(), builder))
                        .then(Commands.argument("ratePercent", DoubleArgumentType.doubleArg(0.0, 100.0))
                                .then(Commands.argument("thresholdMinor", LongArgumentType.longArg(0))
                                        .then(Commands.argument("exemptionMinor", LongArgumentType.longArg(0))
                                                .executes(context -> set(context.getSource(),
                                                        StringArgumentType.getString(context, "type"),
                                                        DoubleArgumentType.getDouble(context, "ratePercent"),
                                                        LongArgumentType.getLong(context, "thresholdMinor"),
                                                        LongArgumentType.getLong(context, "exemptionMinor"),
                                                        context.getSource().getServer().overworld().getGameTime(), true)))))));
        dispatcher.register(root);
    }

    private static int list(CommandSourceStack source) {
        var data = TaxRuleSavedData.get(source.getServer());
        for (TaxType type : TaxType.values()) {
            TaxRule rule = data.get(type);
            if (rule == null) rule = TaxRuleService.defaults(type, source.getServer().overworld().getGameTime());
            TaxRule shown = rule;
            source.sendSuccess(() -> Component.literal(shown.type().id() + " | rate="
                    + (shown.rateBasisPoints() / 100.0) + "% | threshold=" + shown.thresholdMinor()
                    + " | exemption=" + shown.exemptionMinor() + " | effective=" + shown.effectiveFrom()
                    + " | enabled=" + shown.enabled()), false);
        }
        return TaxType.values().length;
    }

    private static int set(CommandSourceStack source, String typeId, double ratePercent,
                            long threshold, long exemption, long effectiveFrom, boolean enabled) {
        TaxType type = TaxType.byId(typeId);
        if ("other".equals(type.id()) && !"other".equals(typeId)) {
            source.sendFailure(Component.literal("Unknown tax type: " + typeId));
            return 0;
        }
        int bps = (int) Math.round(ratePercent * 100.0);
        TaxRuleSavedData.get(source.getServer()).put(new TaxRule(type, bps, threshold, exemption, effectiveFrom, enabled,
                java.util.UUID.randomUUID().toString(), source.getServer().overworld().getGameTime(), source.getTextName()));
        source.sendSuccess(() -> Component.literal("Tax rule updated: " + type.id()), true);
        return 1;
    }

    private static int history(CommandSourceStack source, String typeId) {
        TaxType type = TaxType.byId(typeId);
        if ("other".equals(type.id()) && !"other".equals(typeId)) {
            source.sendFailure(Component.literal("Unknown tax type: " + typeId));
            return 0;
        }
        var history = TaxRuleSavedData.get(source.getServer()).history(type);
        for (TaxRule rule : history) {
            source.sendSuccess(() -> Component.literal(rule.versionId() + " | effective=" + rule.effectiveFrom()
                    + " | rate=" + (rule.rateBasisPoints() / 100.0) + "% | by=" + rule.createdBy()), false);
        }
        return history.size();
    }

    private static int reset(CommandSourceStack source, String typeId) {
        TaxType type = TaxType.byId(typeId);
        if ("other".equals(type.id()) && !"other".equals(typeId)) {
            source.sendFailure(Component.literal("Unknown tax type: " + typeId));
            return 0;
        }
        TaxRuleSavedData.get(source.getServer()).put(TaxRuleService.defaults(type,
                source.getServer().overworld().getGameTime()));
        source.sendSuccess(() -> Component.literal("Tax rule reset to config default: " + type.id()), true);
        return 1;
    }

    private static String[] ids() {
        TaxType[] types = TaxType.values();
        String[] ids = new String[types.length];
        for (int i = 0; i < types.length; i++) ids[i] = types[i].id();
        return ids;
    }
}
