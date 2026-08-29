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

public class PayCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("currency", StringArgumentType.word())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                            String currencyId = StringArgumentType.getString(ctx, "currency");
                                            long amount = LongArgumentType.getLong(ctx, "amount");

                                            if (!Currencies.exists(currencyId)) {
                                                ctx.getSource().sendFailure(Component.translatable("command.capitalismmod.unknown_currency"));
                                                return 0;
                                            }
                                            Currency currency = Currencies.byId(currencyId);
                                            if (!EconomyHelper.tryPay(sender, currency, Money.toMinor(amount))) {
                                                ctx.getSource().sendFailure(Component.translatable("command.capitalismmod.insufficient"));
                                                return 0;
                                            }
                                            EconomyHelper.giveMoney(target, currency, Money.toMinor(amount));

                                            sender.sendSystemMessage(Component.translatable("command.capitalismmod.pay_success",
                                                    amount, Component.translatable(currency.nameKey()), target.getDisplayName()));
                                            target.sendSystemMessage(Component.translatable("command.capitalismmod.received",
                                                    amount, Component.translatable(currency.nameKey()), sender.getDisplayName()));
                                            return 1;
                                        })))));
    }
}
