package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanyTypes;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public class CompanyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("company")
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("upgrade")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> upgrade(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("withdraw")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("currency", StringArgumentType.word())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                .executes(ctx -> withdraw(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name"),
                                                        StringArgumentType.getString(ctx, "currency"),
                                                        LongArgumentType.getLong(ctx, "amount"))))))));
    }

    private static int list(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Map<String, Company> companies = CompanyHelper.getCompanies(player);
        if (companies.isEmpty()) {
            source.sendFailure(Component.translatable("command.capitalismmod.no_companies"));
            return 0;
        }
        for (Company company : companies.values()) {
            player.sendSystemMessage(Component.translatable("command.capitalismmod.company_line",
                    company.level(), company.name(),
                    Component.translatable(CompanyTypes.nameKey(company.type())),
                    company.treasuryOf("usd"), Component.translatable(Currencies.USD.nameKey())));
        }
        return companies.size();
    }

    private static int upgrade(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (CompanyHelper.getCompany(player, name) == null) {
            source.sendFailure(Component.translatable("command.capitalismmod.company_not_found", name));
            return 0;
        }
        if (!CompanyHelper.upgrade(player, name)) {
            source.sendFailure(Component.translatable("command.capitalismmod.insufficient"));
            return 0;
        }
        player.sendSystemMessage(Component.translatable("command.capitalismmod.company_upgraded",
                name, CompanyHelper.getCompany(player, name).level()));
        return 1;
    }

    private static int withdraw(CommandSourceStack source, String name, String currencyId, long amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!Currencies.exists(currencyId)) {
            source.sendFailure(Component.translatable("command.capitalismmod.unknown_currency"));
            return 0;
        }
        if (!CompanyHelper.withdraw(player, name, currencyId, amount)) {
            source.sendFailure(Component.translatable("command.capitalismmod.insufficient"));
            return 0;
        }
        Currency currency = Currencies.byId(currencyId);
        player.sendSystemMessage(Component.translatable("command.capitalismmod.company_withdrawn",
                name, amount, Component.translatable(currency.nameKey())));
        return 1;
    }
}
