package com.ailudick.capitalismmod.supply;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyEconomy;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanyInventoryCostSavedData;
import com.ailudick.capitalismmod.company.CompanyLifecycleService;
import com.ailudick.capitalismmod.company.CompanyQualitySavedData;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.business.IndividualBusinessHelper;
import com.ailudick.capitalismmod.business.IndividualBusiness;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.market.LogisticsSavedData;
import com.ailudick.capitalismmod.market.LogisticsCostSavedData;
import com.ailudick.capitalismmod.market.TradeRegion;
import com.ailudick.capitalismmod.market.TransportMode;
import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.ailudick.capitalismmod.tax.TaxTransactionService;
import com.ailudick.capitalismmod.tax.TaxType;
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
        if (!CompanyLifecycleService.canOperate(player.getServer(), company.companyId())) return false;
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
        CompanyQualitySavedData.ProductQuality quality = CompanyQualitySavedData.get(player.getServer())
                .get(company.companyId(), itemId);
        if (quality != null && quality.units() > 0L
                && quality.averageScore() < Config.COMPANY_QUALITY_RELEASE_THRESHOLD.get()) {
            return false;
        }
        int qualityScore = quality == null ? 0 : quality.averageScore();
        data.addOffer(new SupplyOffer(UUID.randomUUID().toString(), player.getUUID(), companyName, itemId, price,
                TradeRegion.of(player.blockPosition()), qualityScore));
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
        return placeOrder(buyer, offerId, quantity, "");
    }

    /** Places an order and records its cost against the selected buyer company. */
    public static boolean placeOrder(ServerPlayer buyer, String offerId, int quantity, String companyName) {
        if (quantity <= 0) {
            return false;
        }
        String selectedCompanyName = companyName == null ? "" : companyName.trim();
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
        if (total < 0L || Money.toMinor(total) <= 0L) {
            return false;
        }

        Company buyerCompany = selectedCompanyName.isBlank()
                ? null : CompanyHelper.getCompany(buyer, selectedCompanyName);
        if (!selectedCompanyName.isBlank() && (buyerCompany == null
                || !CompanyLifecycleService.canOperate(buyer.getServer(), buyerCompany.companyId()))) {
            // The company name is client-provided; an unknown or suspended company cannot procure.
            return false;
        }
        String supplyOrderId = UUID.randomUUID().toString();
        String buyerCompanyId = buyerCompany == null ? "" : buyerCompany.companyId();
        String orderSource = "supply_order:" + supplyOrderId;
        long inputCreditMinor = 0L;
        if (buyerCompany != null) {
            if (!CompanyHelper.debitTreasuryNonOperating(buyer.getServer(), buyerCompany.companyId(), Currencies.USD.id(),
                    total, "supply_purchase", "采购原料并取得存货")) {
                return false;
            }
        } else if (!EconomyHelper.tryPay(buyer, Currencies.USD, Money.toMinor(total))) {
            return false;
        }
        SupplyOrderAuditService.record(buyer.getServer(), supplyOrderId, "CREATED", buyer.getUUID(),
                offer.ownerUuid(), offer.itemId(), quantity, total);
        if (buyerCompany != null) {
            inputCreditMinor = TaxTransactionService.recordInputCredit(buyer.getServer(), buyerCompany.ownerUuid(),
                    Currencies.USD.id(), Money.toMinorSaturated(total), orderSource,
                    buyer.getServer().overworld().getGameTime());
        } else {
            IndividualBusiness business = IndividualBusinessHelper.get(buyer);
            if (business != null && "active".equals(business.status())) {
                IndividualBusinessHelper.recordTaxableExpense(buyer, business, orderSource, total,
                        buyer.getServer().overworld().getGameTime(), offer.itemId() + " x" + quantity
                                + " from " + offer.companyName());
                inputCreditMinor = TaxTransactionService.recordInputCredit(buyer.getServer(), business.ownerUuid(),
                        Currencies.USD.id(), Money.toMinorSaturated(total), orderSource,
                        buyer.getServer().overworld().getGameTime());
            }
        }

        WarehouseSavedData warehouse = WarehouseSavedData.get(buyer.getServer());
        Company supplierCompany = CompanyHelper.findCompany(buyer.getServer(), offer.ownerUuid(), offer.companyName());
        InventoryOwner supplierOwner = supplierCompany == null
                ? InventoryOwner.player(offer.ownerUuid()) : InventoryOwner.company(supplierCompany.companyId());
        int stock = warehouse.count(supplierOwner, offer.itemId());
        int filled = Math.min(quantity, stock);
        if (filled > 0) {
            warehouse.consume(supplierOwner, item, filled);
            if (supplierCompany != null) {
                CompanyHelper.recordInventorySale(buyer.getServer(), supplierCompany.companyId(),
                        offer.itemId(), filled, orderSource);
            }
            String destination = TradeRegion.of(buyer.blockPosition());
            deliverOrShip(buyer.getServer(), buyer.getUUID(), item, filled, offer.region(),
                    destination, supplyOrderId, buyerCompanyId, offer.price(), offer.ownerUuid());
            if (!buyerCompanyId.isBlank()) {
                CompanyInventoryCostSavedData.get(buyer.getServer()).add(buyerCompanyId, offer.itemId(), filled,
                        EconomyMath.multiply(offer.price(), filled));
            }
            SupplyOrderAuditService.record(buyer.getServer(), supplyOrderId,
                    TradeRegion.distance(offer.region(), destination) == 0 ? "DELIVERED" : "DISPATCHED",
                    buyer.getUUID(),
                    offer.ownerUuid(), offer.itemId(), filled, EconomyMath.multiply(offer.price(), filled));
        }
        if (filled > 0) {
            paySupplier(buyer.getServer(), offer.ownerUuid(), offer.companyName(),
                    EconomyMath.multiply(offer.price(), filled), orderSource);
        }

        int remaining = quantity - filled;
        if (remaining > 0) {
            PurchaseOrder backorder = new PurchaseOrder(supplyOrderId, buyer.getUUID(),
                    offer.ownerUuid(), offer.companyName(), offer.itemId(), remaining, offer.region(),
                    TradeRegion.of(buyer.blockPosition()), offer.price(),
                    buyer.getServer().overworld().getGameTime(), buyerCompanyId, offer.qualityScore())
                    .withOriginalQuantity(quantity)
                    .withInputCreditMinor(inputCreditMinor);
            data.addOrder(backorder);
            SupplyOrderAuditService.record(buyer.getServer(), supplyOrderId, "BACKORDERED", buyer.getUUID(),
                    offer.ownerUuid(), offer.itemId(), remaining, EconomyMath.multiply(offer.price(), remaining));
        } else {
            SupplyOrderAuditService.record(buyer.getServer(), supplyOrderId, "FULFILLED", buyer.getUUID(),
                    offer.ownerUuid(), offer.itemId(), quantity, total);
        }
        return true;
    }

    /** Expires stale paid backorders and returns the undelivered balance to the original payer. */
    public static void expireOrders(MinecraftServer server, long now) {
        int expiryDays = Config.SUPPLY_ORDER_EXPIRY_DAYS.get();
        if (expiryDays <= 0) {
            return;
        }
        long lifetime = PerpetualCalendar.ticksForDays(expiryDays);
        SupplyMarketSavedData data = SupplyMarketSavedData.get(server);
        SupplySettlementSavedData settlements = SupplySettlementSavedData.get(server);
        for (PurchaseOrder order : new ArrayList<>(data.orders())) {
            // Legacy orders have no reliable price or creation time and are retained for admin repair.
            if (order.createdAt() <= 0L || order.unitPrice() <= 0L
                    || now < order.createdAt() || now - order.createdAt() < lifetime) {
                continue;
            }
            long refund = EconomyMath.multiply(order.unitPrice(), order.remaining());
            long refundMinor = refund < 0L ? -1L : Money.toMinor(refund);
            if (refundMinor < 0L) {
                continue;
            }
            if (settlements.hasOrderRefund(order.id())) {
                data.removeOrder(order.id());
                continue;
            }
            boolean refundedToCompany = false;
            if (order.buyerCompanyId() != null && !order.buyerCompanyId().isBlank()) {
                Company buyerCompany = CompanySavedData.get(server).get(order.buyerCompanyId());
                refundedToCompany = buyerCompany != null
                        && CompanyHelper.creditTreasuryNonOperating(server, buyerCompany.companyId(),
                        Currencies.USD.id(), refund, "supply_refund", "Expired undelivered supply order refund");
            }
            if (!refundedToCompany) {
                MarketMailboxSavedData.get(server).creditMoney(order.buyerUuid(), Currencies.USD.id(), refundMinor);
            }
            long creditToReverse = proportionalInputCredit(order);
            if (creditToReverse > 0L) {
                TaxTransactionService.reverseInputCredit(server, order.buyerUuid(), Currencies.USD.id(),
                        creditToReverse, "supply_order:" + order.id(), now);
            }
            settlements.recordOrderRefund(order.id());
            SupplyOrderAuditService.record(server, order,
                    refundedToCompany ? "EXPIRED_REFUND_COMPANY" : "EXPIRED_REFUND",
                    order.remaining(), refund);
            data.removeOrder(order.id());
        }
    }

    private static long proportionalInputCredit(PurchaseOrder order) {
        if (order == null || order.inputCreditMinor() <= 0L || order.remaining() <= 0
                || order.originalQuantity() <= 0) return 0L;
        try {
            return Math.multiplyExact(order.inputCreditMinor(), (long) order.remaining())
                    / order.originalQuantity();
        } catch (ArithmeticException e) {
            return order.inputCreditMinor();
        }
    }

    /** Delivers backorders to buyers from the supplier's current stock. Called after production. */
    public static void fulfill(MinecraftServer server, UUID supplierUuid, String itemId) {
        fulfill(server, InventoryOwner.player(supplierUuid), supplierUuid, itemId);
    }

    public static void fulfill(MinecraftServer server, InventoryOwner supplierOwner, UUID supplierUuid, String itemId) {
        SupplyMarketSavedData data = SupplyMarketSavedData.get(server);
        SupplyDeliverySavedData deliveryJournal = SupplyDeliverySavedData.get(server);
        Item item = parseItem(itemId);
        if (item == null) {
            return;
        }
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        for (PurchaseOrder order : new ArrayList<>(data.orders())) {
            if (!order.supplierUuid().equals(supplierUuid) || !order.itemId().equals(itemId)) {
                continue;
            }
            String deliveryKey = order.id() + ":delivery:" + order.remaining();
            SupplyDeliverySavedData.Delivery recorded = deliveryJournal.find(deliveryKey);
            if (recorded != null) {
                // The goods were already dispatched; recover the order state without
                // consuming fresh stock. The payment key makes this retry safe too.
                paySupplier(server, order.supplierUuid(), order.companyName(),
                        EconomyMath.multiply(order.unitPrice(), recorded.quantity()), deliveryKey);
                applyDeliveryProgress(server, data, order, recorded.remaining(), recorded.quantity(), false);
                continue;
            }
            Company supplierCompany = CompanyHelper.findCompany(server, supplierUuid, order.companyName());
            InventoryOwner resolvedOwner = supplierCompany == null ? supplierOwner
                    : InventoryOwner.company(supplierCompany.companyId());
            int stock = warehouse.count(resolvedOwner, itemId);
            int deliver = Math.min(order.remaining(), stock);
            if (deliver <= 0) {
                continue;
            }
            warehouse.consume(resolvedOwner, item, deliver);
            if (supplierCompany != null) {
                CompanyHelper.recordInventorySale(server, supplierCompany.companyId(),
                        order.itemId(), deliver, order.id() + ":delivery:" + order.remaining());
            }
            String deliveryType = TradeRegion.distance(order.originRegion(), order.destinationRegion()) == 0
                    ? "DELIVERED" : "DISPATCHED";
            deliverOrShip(server, order.buyerUuid(), item, deliver, order.originRegion(), order.destinationRegion(),
                    order.id(), order.buyerCompanyId(), order.unitPrice(), order.supplierUuid());
            if (!order.buyerCompanyId().isBlank()) {
                CompanyInventoryCostSavedData.get(server).add(order.buyerCompanyId(), order.itemId(), deliver,
                        EconomyMath.multiply(order.unitPrice(), deliver));
            }
            int newRemaining = order.remaining() - deliver;
            // Buyer funds for a backorder are held until this portion is
            // dispatched. The undelivered remainder remains refundable.
            paySupplier(server, order.supplierUuid(), order.companyName(),
                    EconomyMath.multiply(order.unitPrice(), deliver),
                    deliveryKey);
            deliveryJournal.record(new SupplyDeliverySavedData.Delivery(deliveryKey, order.id(), deliver,
                    newRemaining));
            SupplyOrderAuditService.record(server, order, deliveryType, deliver,
                    EconomyMath.multiply(order.unitPrice(), deliver));
            applyDeliveryProgress(server, data, order, newRemaining, deliver, true);
        }
    }

    private static void applyDeliveryProgress(MinecraftServer server, SupplyMarketSavedData data,
                                              PurchaseOrder order, int newRemaining, int delivered,
                                              boolean writeAudit) {
        if (newRemaining <= 0) {
            if (writeAudit) {
                SupplyOrderAuditService.record(server, order, "FULFILLED", delivered,
                        EconomyMath.multiply(order.unitPrice(), delivered));
            }
            data.removeOrder(order.id());
        } else {
            data.replaceOrder(order.withRemaining(newRemaining));
            if (writeAudit) {
                SupplyOrderAuditService.record(server, order.withRemaining(newRemaining), "PARTIAL", newRemaining,
                        EconomyMath.multiply(order.unitPrice(), newRemaining));
            }
        }
    }

    private static void paySupplier(MinecraftServer server, UUID supplierUuid, String companyName,
                                    long amount, String sourceId) {
        if (server == null || supplierUuid == null || amount <= 0L) return;
        SupplySettlementSavedData settlements = SupplySettlementSavedData.get(server);
        if (settlements.hasSupplierPayment(sourceId)) return;
        long now = server.overworld().getGameTime();
        TaxTransactionService.assess(server, TaxType.VAT, supplierUuid, Currencies.USD.id(),
                Money.toMinorSaturated(amount), "supply-sale:" + sourceId, now);
        Company company = CompanyHelper.findCompany(server, supplierUuid, companyName);
        if (company != null && CompanyHelper.creditTreasury(server, company.companyId(), Currencies.USD.id(), amount)) {
            CompanyHelper.recordTaxableIncome(server, company, "supply_sale:" + sourceId,
                    amount, Currencies.USD.id(), now);
            settlements.recordSupplierPayment(sourceId);
            return;
        }
        ServerPlayer supplier = server.getPlayerList().getPlayer(supplierUuid);
        if (supplier != null) {
            EconomyHelper.giveMoney(supplier, Currencies.USD, Money.toMinor(amount));
        } else {
            MarketMailboxSavedData.get(server).creditMoney(supplierUuid, "usd", Money.toMinor(amount));
        }
        settlements.recordSupplierPayment(sourceId);
    }

    private static void deliverOrShip(MinecraftServer server, UUID buyer, Item item, int quantity,
                                      String origin, String destination, String supplyOrderId,
                                      String buyerCompanyId, long unitPrice, UUID supplierUuid) {
        if (quantity <= 0) {
            return;
        }
        if (TradeRegion.distance(origin, destination) == 0) {
            InventoryOwner owner = buyerCompanyId == null || buyerCompanyId.isBlank()
                    ? InventoryOwner.player(buyer) : InventoryOwner.company(buyerCompanyId);
            WarehouseSavedData.get(server).credit(owner, item, quantity);
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
                    origin, destination, transport, false, 0, supplyOrderId, buyerCompanyId, unitPrice,
                    supplierUuid));
            String shipmentId = data.shipments().get(data.shipments().size() - 1).id();
            int fuelUnits = transport.estimatedFuelUnits(batch, distance);
            long fuelUnitPrice = Math.max(0L, com.ailudick.capitalismmod.market.CommoditySavedData
                    .get(server).price(transport.fuelItemId()));
            long estimatedFuelCost;
            try {
                estimatedFuelCost = Math.multiplyExact((long) fuelUnits, fuelUnitPrice);
            } catch (ArithmeticException e) {
                estimatedFuelCost = Long.MAX_VALUE;
            }
            LogisticsCostSavedData.get(server).record(new LogisticsCostSavedData.FuelPlan(
                    shipmentId, buyer, itemId, batch, origin, destination, transport,
                    transport.fuelItemId(), fuelUnits, fuelUnitPrice, estimatedFuelCost,
                    server.overworld().getGameTime(), buyerCompanyId));
            remaining -= batch;
        }
    }

    private static Item parseItem(String itemId) {
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            return (item == null || item == Items.AIR) ? null : item;
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
