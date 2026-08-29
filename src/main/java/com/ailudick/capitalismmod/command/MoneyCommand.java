package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class MoneyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("money")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("currency", StringArgumentType.word())
                                .then(Commands.argument("amount", LongArgumentType.longArg())
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                            String currencyId = StringArgumentType.getString(ctx, "currency");
                                            long amount = LongArgumentType.getLong(ctx, "amount");

                                            if (!Currencies.exists(currencyId)) {
                                                ctx.getSource().sendFailure(Component.translatable("command.capitalismmod.unknown_currency"));
                                                return 0;
                                            }
                                            Currency currency = Currencies.byId(currencyId);
                                            EconomyHelper.giveMoney(target, currency, Money.toMinor(amount));

                                            target.sendSystemMessage(Component.translatable("command.capitalismmod.money_set",
                                                    amount, Component.translatable(currency.nameKey()), Money.format(EconomyHelper.getBalance(target, currency))));
                                            return 1;
                                        })))));
    }
}
