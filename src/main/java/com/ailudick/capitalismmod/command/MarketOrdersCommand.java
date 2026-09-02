package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.auction.Auction;
import com.ailudick.capitalismmod.auction.AuctionSavedData;
import com.ailudick.capitalismmod.economy.EconomySavedData;
import com.ailudick.capitalismmod.market.CommodityMarket;
import com.ailudick.capitalismmod.market.MarketOrder;
import com.ailudick.capitalismmod.stock.StockOrder;
import com.ailudick.capitalismmod.supply.PurchaseOrder;
import com.ailudick.capitalismmod.supply.SupplyMarketSavedData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;

/** Lists active orders across all markets without changing escrowed assets. */
public final class MarketOrdersCommand {
    private MarketOrdersCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("marketorders")
                .executes(ctx -> list(ctx.getSource(), false))
                .then(Commands.literal("all")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> list(ctx.getSource(), true))));
    }

    private static int list(CommandSourceStack source, boolean all) {
        ServerPlayer player = source.getEntity() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        MinecraftServer server = source.getServer();
        String owner = player == null ? "" : player.getUUID().toString();
        int count = 0;

        for (StockOrder order : EconomySavedData.get(server).orders()) {
            if (all || order.ownerId().equals(owner)) {
                source.sendSuccess(() -> Component.literal("股票 " + order.id() + " " + order.stockId()
                        + " 数量=" + order.quantity() + " 价格=" + order.pricePerUnit()), false);
                count++;
            }
        }
        for (MarketOrder order : CommodityMarket.getOrders(server)) {
            if (all || order.ownerId().equals(owner)) {
                String itemId = BuiltInRegistries.ITEM.getKey(order.commodity().getItem()).toString();
                source.sendSuccess(() -> Component.literal("商品 " + order.id() + " " + itemId
                        + " 数量=" + order.quantity() + " 价格=" + order.pricePerUnit()), false);
                count++;
            }
        }
        for (PurchaseOrder order : SupplyMarketSavedData.get(server).orders()) {
            if (all || order.buyerUuid().toString().equals(owner)) {
                source.sendSuccess(() -> Component.literal("采购 " + order.id() + " " + order.itemId()
                        + " 剩余=" + order.remaining()), false);
                count++;
            }
        }
        for (Auction auction : AuctionSavedData.get(server).auctions()) {
            if (all || auction.seller().toString().equals(owner)) {
                source.sendSuccess(() -> Component.literal("拍卖 " + auction.id() + " 数量=" + auction.quantity()
                        + " 当前价=" + auction.currentBid()), false);
                count++;
            }
        }
        if (count == 0) {
            source.sendSuccess(() -> Component.literal(all ? "当前没有活动订单。" : "你没有活动订单。"), false);
        }
        return count;
    }
}
