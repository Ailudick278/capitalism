package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.supply.SupplyMarket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /offer &lt;company&gt; &lt;item&gt; &lt;price&gt; — lists one of the company's produced commodities for sale.
 */
public class OfferCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("offer")
                .then(Commands.argument("company", StringArgumentType.word())
                        .then(Commands.argument("item", StringArgumentType.word())
                                .then(Commands.argument("price", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String company = StringArgumentType.getString(ctx, "company");
                                            String item = StringArgumentType.getString(ctx, "item");
                                            long price = LongArgumentType.getLong(ctx, "price");
                                            if (!SupplyMarket.listOffer(player, company, item, price)) {
                                                ctx.getSource().sendFailure(Component.translatable("command.capitalismmod.offer_failed"));
                                                return 0;
                                            }
                                            player.sendSystemMessage(Component.translatable("command.capitalismmod.offer_made", item, price));
                                            return 1;
                                        })))));
    }
}
