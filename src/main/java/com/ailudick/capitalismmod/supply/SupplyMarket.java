package com.ailudick.capitalismmod.supply;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyEconomy;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.market.LogisticsSavedData;
import com.ailudick.capitalismmod.market.TradeRegion;
import com.ailudick.capitalismmod.market.TransportMode;
import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Server-side B2B supply market: suppliers list fixed-price offers for commodities
 * their companies produce; buyers place orders that fill immediately from stock and
 * backorder the rest for later delivery as the supplier produces more.
 */
public final class SupplyMarket {
    private SupplyMarket() {
    }

    /** Lists a commodity for sale by the player's company (must be one of the company's outputs). */
    public static boolean listOffer(ServerPlayer player, String companyName, String itemId, long price) {
        if (price <= 0) {
            return false;
        }
        Company company = CompanyHelper.getCompany(player, companyName);
        if (company == null) {
            return false;
        }
        if (!CompanyEconomy.outputs(company).containsKey(itemId)) {
            return false;
        }
        SupplyMarketSavedData data = SupplyMarketSavedData.get(player.getServer());
        // overwrite an existing listing for the same company + commodity
        for (SupplyOffer offer : new ArrayList<>(data.offers())) {
            if (offer.ownerUuid().equals(player.getUUID())
                    && offer.companyName().equals(companyName)
                    && offer.itemId().equals(itemId)) {
                data.removeOffer(offer.id());
            }
        }
        data.addOffer(new SupplyOffer(UUID.randomUUID().toString(), player.getUUID(), companyName, itemId, price,
                TradeRegion.of(player.blockPosition())));
        return true;
    }

    public static void removeOffersForCompany(MinecraftServer server, UUID ownerUuid, String companyName) {
        SupplyMarketSavedData data = SupplyMarketSavedData.get(server);
        for (SupplyOffer offer : new ArrayList<>(data.offers())) {
            if (offer.ownerUuid().equals(ownerUuid) && offer.companyName().equals(companyName)) {
                data.removeOffer(offer.id());
            }
        }
    }

    /** Places a buy order: pays up front, fills from supplier stock, backorders the rest. */
    public static boolean placeOrder(ServerPlayer buyer, String offerId, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        SupplyMarketSavedData data = SupplyMarketSavedData.get(buyer.getServer());
        SupplyOffer offer = data.findOffer(offerId);
        if (offer == null) {
            return false;
        }
        Item item = parseItem(offer.itemId());
        if (item == null) {
            return false;
        }
        long total = EconomyMath.multiply(offer.price(), quantity);
        if (total < 0 || !EconomyHelper.tryPay(buyer, Currencies.USD, Money.toMinor(total))) {
            return false;
        }

        WarehouseSavedData warehouse = WarehouseSavedData.get(buyer.getServer());
        int stock = warehouse.count(offer.ownerUuid(), offer.itemId());
        int filled = Math.min(quantity, stock);
        if (filled > 0) {
            warehouse.consume(offer.ownerUuid(), item, filled);
            deliverOrShip(buyer.getServer(), buyer.getUUID(), item, filled, offer.region(),
                    TradeRegion.of(buyer.blockPosition()));
        }
        paySupplier(buyer.getServer(), offer.ownerUuid(), total);

        int remaining = quantity - filled;
        if (remaining > 0) {
            data.addOrder(new PurchaseOrder(UUID.randomUUID().toString(), buyer.getUUID(),
                    offer.ownerUuid(), offer.companyName(), offer.itemId(), remaining, offer.region(),
                    TradeRegion.of(buyer.blockPosition())));
        }
        return true;
    }

    /** Delivers backorders to buyers from the supplier's current stock. Called after production. */
    public static void fulfill(MinecraftServer server, UUID supplierUuid, String itemId) {
        SupplyMarketSavedData data = SupplyMarketSavedData.get(server);
        Item item = parseItem(itemId);
        if (item == null) {
            return;
        }
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        for (PurchaseOrder order : new ArrayList<>(data.orders())) {
            if (!order.supplierUuid().equals(supplierUuid) || !order.itemId().equals(itemId)) {
                continue;
            }
            int stock = warehouse.count(supplierUuid, itemId);
            int deliver = Math.min(order.remaining(), stock);
            if (deliver <= 0) {
                continue;
            }
            warehouse.consume(supplierUuid, item, deliver);
            deliverOrShip(server, order.buyerUuid(), item, deliver, order.originRegion(), order.destinationRegion());
            int newRemaining = order.remaining() - deliver;
            if (newRemaining <= 0) {
                data.removeOrder(order.id());
            } else {
                data.replaceOrder(order.withRemaining(newRemaining));
            }
        }
    }

    private static void paySupplier(MinecraftServer server, UUID supplierUuid, long amount) {
        ServerPlayer supplier = server.getPlayerList().getPlayer(supplierUuid);
        if (supplier != null) {
            EconomyHelper.giveMoney(supplier, Currencies.USD, Money.toMinor(amount));
        } else {
            MarketMailboxSavedData.get(server).creditMoney(supplierUuid, "usd", Money.toMinor(amount));
        }
    }

    private static void deliverOrShip(MinecraftServer server, UUID buyer, Item item, int quantity,
                                      String origin, String destination) {
        if (quantity <= 0) {
            return;
        }
        if (TradeRegion.distance(origin, destination) == 0) {
            WarehouseSavedData.get(server).credit(buyer, item, quantity);
            return;
        }
        long distance = TradeRegion.distance(origin, destination);
        TransportMode transport = TransportMode.forDistance(distance);
        LogisticsInfrastructureSavedData infrastructure = LogisticsInfrastructureSavedData.get(server);
        long delay;
        try {
            delay = transport.travelTicks(Config.REGIONAL_SHIPPING_TICKS.get(), distance);
            delay = infrastructure.adjustTravelTicks(delay, origin, destination, transport);
            delay = Math.addExact(server.overworld().getGameTime(), delay);
        } catch (ArithmeticException e) {
            delay = Long.MAX_VALUE;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        LogisticsSavedData data = LogisticsSavedData.get(server);
        int remaining = quantity;
        int capacity = transport.capacity() + infrastructure.capacityBonus(origin, destination, transport);
        while (remaining > 0) {
            int batch = Math.min(remaining, capacity);
            data.add(new LogisticsSavedData.Shipment(UUID.randomUUID().toString(), buyer, itemId, batch, delay,
                    origin, destination, transport, false));
            remaining -= batch;
        }
    }

    private static Item parseItem(String itemId) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        return (item == null || item == Items.AIR) ? null : item;
    }
}
