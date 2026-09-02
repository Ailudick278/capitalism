package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.auction.Auction;
import com.ailudick.capitalismmod.auction.AuctionSavedData;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.economy.EconomySavedData;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.market.MarketOrder;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.stock.StockOrder;
import com.ailudick.capitalismmod.supply.PurchaseOrder;
import com.ailudick.capitalismmod.supply.SupplyMarketSavedData;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.UUID;

/** Admin-only, conservative repair for malformed escrow orders. */
public final class MarketRepairCommand {
    private MarketRepairCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("marketrepair")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("scan").executes(ctx -> scan(ctx.getSource())))
                .then(Commands.literal("fix").executes(ctx -> fix(ctx.getSource()))));
    }

    private static int scan(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        int invalid = 0;
        for (StockOrder order : EconomySavedData.get(server).orders()) {
            invalid += invalidStock(order) ? 1 : 0;
        }
        for (MarketOrder order : CommoditySavedData.get(server).orders()) {
            invalid += invalidCommodity(order) ? 1 : 0;
        }
        for (PurchaseOrder order : SupplyMarketSavedData.get(server).orders()) {
            invalid += order.remaining() <= 0 || order.itemId().isBlank() ? 1 : 0;
        }
        for (Auction auction : AuctionSavedData.get(server).auctions()) {
            invalid += invalidAuction(auction) ? 1 : 0;
        }
        int result = invalid;
        source.sendSuccess(() -> Component.literal("异常订单候选: " + result
                + "。采购订单不会自动修复，因为存档没有保留历史成交价。"), false);
        return result;
    }

    private static int fix(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        int fixed = 0;
        EconomySavedData stocks = EconomySavedData.get(server);
        for (StockOrder order : new ArrayList<>(stocks.orders())) {
            if (!invalidStock(order) || !validUuid(order.ownerId()) || order.quantity() <= 0
                    || !stocks.isStock(order.stockId())) {
                continue;
            }
            UUID owner = UUID.fromString(order.ownerId());
            stocks.removeOrder(order.id());
            if (order.sell()) {
                stocks.addShares(order.stockId(), owner, order.quantity());
            } else {
                refund(server, owner, order.quantity(), order.pricePerUnit());
            }
            fixed++;
        }
        CommoditySavedData commodities = CommoditySavedData.get(server);
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        for (MarketOrder order : new ArrayList<>(commodities.orders())) {
            if (!invalidCommodity(order) || !validUuid(order.ownerId()) || order.quantity() <= 0) {
                continue;
            }
            Item item = order.commodity().getItem();
            if (item == Items.AIR) {
                continue;
            }
            UUID owner = UUID.fromString(order.ownerId());
            commodities.removeOrder(order.id());
            if (order.sell()) {
                warehouse.credit(owner, item, order.quantity());
            } else {
                refund(server, owner, order.quantity(), order.pricePerUnit());
            }
            fixed++;
        }
        AuctionSavedData auctions = AuctionSavedData.get(server);
        for (Auction auction : new ArrayList<>(auctions.auctions())) {
            if (!invalidAuction(auction) || auction.seller() == null || auction.quantity() <= 0) {
                continue;
            }
            Item item = parseItem(auction.itemId());
            if (item == Items.AIR) {
                continue;
            }
            auctions.removeAuction(auction.id());
            warehouse.credit(auction.seller(), item, auction.quantity());
            if (auction.currentBid() > 0 && validUuid(auction.currentBidder())) {
                MarketMailboxSavedData.get(server).creditMoney(UUID.fromString(auction.currentBidder()),
                        Currencies.USD.id(), Money.toMinor(auction.currentBid()));
            }
            fixed++;
        }
        int result = fixed;
        source.sendSuccess(() -> Component.literal("已安全修复订单: " + result), false);
        return result;
    }

    private static boolean invalidStock(StockOrder order) {
        return order.quantity() <= 0 || order.pricePerUnit() <= 0 || !validUuid(order.ownerId())
                || order.stockId().isBlank();
    }

    private static boolean invalidCommodity(MarketOrder order) {
        return order.quantity() <= 0 || order.pricePerUnit() <= 0 || !validUuid(order.ownerId())
                || order.commodity().isEmpty() || order.commodity().is(Items.AIR);
    }

    private static boolean invalidAuction(Auction auction) {
        return auction.quantity() <= 0 || auction.startingPrice() <= 0 || auction.currentBid() < 0
                || auction.endTick() < 0 || auction.itemId().isBlank();
    }

    private static boolean validUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    private static Item parseItem(String itemId) {
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            return item == null ? Items.AIR : item;
        } catch (IllegalArgumentException e) {
            return Items.AIR;
        }
    }

    private static void refund(MinecraftServer server, UUID owner, int quantity, long price) {
        long total = EconomyMath.multiply(quantity, price);
        long minor = total < 0 ? -1 : Money.toMinor(total);
        if (minor > 0) {
            MarketMailboxSavedData.get(server).creditMoney(owner, Currencies.USD.id(), minor);
        }
    }
}
