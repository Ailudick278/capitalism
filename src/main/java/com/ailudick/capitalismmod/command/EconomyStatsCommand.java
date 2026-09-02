package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.auction.AuctionSavedData;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.economy.EconomyLogSavedData;
import com.ailudick.capitalismmod.economy.EconomySavedData;
import com.ailudick.capitalismmod.economy.EconomySettlementSavedData;
import com.ailudick.capitalismmod.economy.MarketTradeSavedData;
import com.ailudick.capitalismmod.market.CommodityMarket;
import com.ailudick.capitalismmod.market.MarketOrder;
import com.ailudick.capitalismmod.auction.Auction;
import com.ailudick.capitalismmod.stock.StockOrder;
import com.ailudick.capitalismmod.supply.PurchaseOrder;
import com.ailudick.capitalismmod.stock.StockMarket;
import com.ailudick.capitalismmod.supply.SupplyMarketSavedData;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

/** Read-only administrator dashboard for diagnosing the in-world economy. */
public final class EconomyStatsCommand {
    private EconomyStatsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("economystats")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> show(ctx.getSource())));
    }

    private static int show(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        EconomySavedData economy = EconomySavedData.get(server);
        long settlementDay = EconomySettlementSavedData.get(server).lastSettlementDay();
        source.sendSuccess(() -> Component.literal("=== 经济统计 ==="), false);
        source.sendSuccess(() -> Component.literal("在线玩家: " + server.getPlayerList().getPlayerCount()
                + "  已结算世界日: " + settlementDay), false);

        for (Currency currency : Currencies.ALL) {
            long total = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                long balance = EconomyHelper.getBalance(player, currency);
                if (Long.MAX_VALUE - total < balance) {
                    total = Long.MAX_VALUE;
                    break;
                }
                total += balance;
            }
            String amount = total == Long.MAX_VALUE ? "溢出" : Money.format(total);
            source.sendSuccess(() -> Component.literal(currency.id() + " 在线流通量: " + amount), false);
        }

        source.sendSuccess(() -> Component.literal("股票订单: " + StockMarket.getOrders(server).size()
                + "  商品订单: " + CommodityMarket.getOrders(server).size()), false);
        source.sendSuccess(() -> Component.literal("采购订单: " + SupplyMarketSavedData.get(server).orders().size()
                + "  拍卖: " + AuctionSavedData.get(server).auctions().size()), false);
        int invalid = 0;
        for (StockOrder order : StockMarket.getOrders(server)) {
            if (order.quantity() <= 0 || order.pricePerUnit() <= 0 || !economy.isStock(order.stockId())) {
                invalid++;
            }
        }
        for (MarketOrder order : CommodityMarket.getOrders(server)) {
            if (order.quantity() <= 0 || order.pricePerUnit() <= 0 || order.commodity().isEmpty()
                    || order.commodity().is(Items.AIR)) {
                invalid++;
            }
        }
        for (PurchaseOrder order : SupplyMarketSavedData.get(server).orders()) {
            if (order.remaining() <= 0 || order.itemId().isBlank()) {
                invalid++;
            }
        }
        for (Auction auction : AuctionSavedData.get(server).auctions()) {
            if (auction.quantity() <= 0 || auction.startingPrice() <= 0 || auction.currentBid() < 0
                    || auction.endTick() < 0) {
                invalid++;
            }
        }
        int invalidOrders = invalid;
        source.sendSuccess(() -> Component.literal("异常订单候选: " + invalidOrders
                + (invalidOrders == 0 ? "（未发现）" : "（请先备份存档后人工处理）")), false);
        source.sendSuccess(() -> Component.literal("钱包流水记录: "
                + EconomyLogSavedData.get(server).entries().size()), false);
        MarketTradeSavedData tradeData = MarketTradeSavedData.get(server);
        long tradeVolume = 0;
        long tradeQuantity = 0;
        long tradeFees = 0;
        for (MarketTradeSavedData.Trade trade : tradeData.trades()) {
            tradeQuantity = safeAdd(tradeQuantity, trade.quantity());
            tradeVolume = safeAdd(tradeVolume, trade.total());
            tradeFees = safeAdd(tradeFees, trade.fee());
        }
        long finalTradeQuantity = tradeQuantity;
        long finalTradeVolume = tradeVolume;
        long finalTradeFees = tradeFees;
        source.sendSuccess(() -> Component.literal("成交记录: " + tradeData.trades().size()
                + "  成交数量: " + finalTradeQuantity + "  成交额: " + finalTradeVolume), false);
        source.sendSuccess(() -> Component.literal("总手续费: " + finalTradeFees), false);
        return 1;
    }

    private static long safeAdd(long left, long right) {
        if (right > Long.MAX_VALUE - left) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
