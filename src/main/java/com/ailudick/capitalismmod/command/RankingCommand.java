package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.bank.BankAccount;
import com.ailudick.capitalismmod.bank.BankAccountHelper;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.futures.FuturesMarket;
import com.ailudick.capitalismmod.market.CommodityMarket;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.stock.StockMarket;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /ranking — aggregates each online player's total net worth (in USD) and prints the top 10.
 */
public class RankingCommand {
    private static final int TOP_N = 10;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ranking").executes(ctx -> {
            CommandSourceStack source = ctx.getSource();
            MinecraftServer server = source.getServer();

            Map<String, Long> wealth = new HashMap<>();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                wealth.put(player.getGameProfile().getName(), netWorth(server, player));
            }

            List<Map.Entry<String, Long>> sorted = new ArrayList<>(wealth.entrySet());
            sorted.sort(Map.Entry.<String, Long>comparingByValue().reversed());

            source.sendSystemMessage(Component.translatable("command.capitalismmod.ranking_title"));
            int rank = 1;
            for (Map.Entry<String, Long> entry : sorted) {
                source.sendSystemMessage(Component.literal(rank + ". " + entry.getKey() + " - $" + entry.getValue()));
                if (++rank > TOP_N) {
                    break;
                }
            }
            return 1;
        }));
    }

    /** Total net worth in USD (major units): wallets + bank + stock + warehouse + futures margin. */
    private static long netWorth(MinecraftServer server, ServerPlayer player) {
        long total = 0L;

        // wallets + bank balances, converted to USD
        for (Currency currency : Currencies.ALL) {
            long wallet = EconomyHelper.countItems(player, currency);
            long bank = 0L;
            for (BankAccount account : BankAccountHelper.getAccounts(player).values()) {
                bank += account.getBalance(currency.id());
            }
            long minor = wallet + bank;
            if (minor > 0) {
                total += ExchangeRates.convert(minor, currency, Currencies.USD) / Money.MINOR_UNITS_PER_UNIT;
            }
        }

        // stock portfolio at market price
        Map<String, Long> prices = StockMarket.getPrices(server);
        for (Map.Entry<String, Long> holding : StockMarket.getPortfolio(server, player).entrySet()) {
            total += holding.getValue() * prices.getOrDefault(holding.getKey(), 0L);
        }

        // warehouse commodities at spot price
        Map<String, Long> spotPrices = CommodityMarket.getPrices(server);
        for (Map.Entry<String, Integer> entry : WarehouseSavedData.get(server).storage(player.getUUID()).entrySet()) {
            total += (long) entry.getValue() * spotPrices.getOrDefault(entry.getKey(), 0L);
        }

        // futures margin balance
        total += FuturesMarket.marginBalance(server, player.getUUID());
        return total;
    }
}
