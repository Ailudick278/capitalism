package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class ExchangeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("exchange")
                .then(Commands.argument("from", StringArgumentType.word())
                        .then(Commands.argument("to", StringArgumentType.word())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String fromId = StringArgumentType.getString(ctx, "from");
                                            String toId = StringArgumentType.getString(ctx, "to");
                                            long amount = LongArgumentType.getLong(ctx, "amount");

                                            if (!Currencies.exists(fromId) || !Currencies.exists(toId)) {
                                                ctx.getSource().sendFailure(Component.translatable("command.capitalismmod.unknown_currency"));
                                                return 0;
                                            }
                                            Currency from = Currencies.byId(fromId);
                                            Currency to = Currencies.byId(toId);
                                            if (from.equals(to)) {
                                                ctx.getSource().sendFailure(Component.translatable("command.capitalismmod.same_currency"));
                                                return 0;
                                            }
                                            long amountMinor = Money.toMinor(amount);
                                            long converted = ExchangeRates.convert(amountMinor, from, to);
                                            if (converted <= 0) {
                                                ctx.getSource().sendFailure(Component.translatable("command.capitalismmod.amount_too_small"));
                                                return 0;
                                            }
                                            if (!EconomyHelper.tryPay(player, from, amountMinor)) {
                                                ctx.getSource().sendFailure(Component.translatable("command.capitalismmod.insufficient"));
                                                return 0;
                                            }

                                            EconomyHelper.giveMoney(player, to, converted);

                                            player.sendSystemMessage(Component.translatable("command.capitalismmod.exchange_success",
                                                    amount, Component.translatable(from.nameKey()), Money.format(converted), Component.translatable(to.nameKey())));
                                            return 1;
                                        })))));
    }
}
