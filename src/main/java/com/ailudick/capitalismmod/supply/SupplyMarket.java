package com.ailudick.capitalismmod.supply;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyEconomy;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanyInventoryCostSavedData;
import com.ailudick.capitalismmod.company.CompanyLifecycleService;
import com.ailudick.capitalismmod.company.CompanyQualitySavedData;
import com.ailudick.capitalismmod.company.CompanyQualityHoldSavedData;
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
import com.ailudick.capitalismmod.economy.EconomyLogSavedData;
import com.ailudick.capitalismmod.economy.FinancialSettlementJournalSavedData;
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
        long settlementTime = buyer.getServer().overworld().getGameTime();
        FinancialSettlementJournalSavedData financialJournal = FinancialSettlementJournalSavedData.get(buyer.getServer());
        long totalMinor = Money.toMinorSaturated(total);
        financialJournal.markStarted(supplyOrderId, "supply", "buyer-payment", totalMinor, settlementTime);
        SupplyOrderIntentSavedData intents = SupplyOrderIntentSavedData.get(buyer.getServer());
        intents.add(new SupplyOrderIntentSavedData.Intent(supplyOrderId, buyer.getUUID(), offer.ownerUuid(),
                offer.companyName(), offer.itemId(), quantity, offer.region(),
                TradeRegion.of(buyer.blockPosition()), offer.price(), buyer.getServer().overworld().getGameTime(),
                buyerCompanyId, offer.qualityScore(), false));
        long inputCreditMinor = 0L;
        if (buyerCompany != null) {
            if (!CompanyHelper.debitTreasuryNonOperatingOnce(buyer.getServer(), buyerCompany.companyId(),
                    Currencies.USD.id(), total, "supply_purchase", "采购原料并取得存货", orderSource)) {
                intents.remove(supplyOrderId);
                return false;
            }
        } else if (!EconomyHelper.tryPayWithReference(buyer, Currencies.USD, Money.toMinor(total), orderSource)) {
            intents.remove(supplyOrderId);
            return false;
        }
        intents.markPaid(supplyOrderId);
        financialJournal.markCompleted(supplyOrderId, "supply", "buyer-payment", totalMinor, settlementTime);
        // Persist the paid order before any inventory, logistics, or tax side
        // effect. If the server stops during fulfillment, the backorder
        // reconciler can continue from this complete snapshot.
        PurchaseOrder paidOrder = new PurchaseOrder(supplyOrderId, buyer.getUUID(),
                offer.ownerUuid(), offer.companyName(), offer.itemId(), quantity, offer.region(),
                TradeRegion.of(buyer.blockPosition()), offer.price(),
                buyer.getServer().overworld().getGameTime(), buyerCompanyId, offer.qualityScore())
                .withOriginalQuantity(quantity);
        financialJournal.markStarted(supplyOrderId, "supply", "order-record", quantity, settlementTime);
        data.addOrder(paidOrder);
        financialJournal.markCompleted(supplyOrderId, "supply", "order-record", quantity, settlementTime);
        intents.remove(supplyOrderId);
        financialJournal.markStarted(supplyOrderId, "supply", "buyer-escrow", totalMinor, settlementTime);
        SupplyEscrowSavedData.get(buyer.getServer()).createOnce(supplyOrderId, quantity, totalMinor);
        financialJournal.markCompleted(supplyOrderId, "supply", "buyer-escrow", totalMinor, settlementTime);
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
        if (supplierCompany != null) {
            stock = CompanyQualityHoldSavedData.get(buyer.getServer())
                    .availableUnits(supplierCompany.companyId(), offer.itemId(), stock);
        }
        String initialDeliveryKey = supplyOrderId + ":delivery:" + quantity;
        int filled = Math.min(quantity, stock);
        if (filled > 0) {
            // Re-check the actual mutation result before creating delivery,
            // inventory-cost, tax, or supplier-payment records. The stock
            // snapshot above can be stale when an order is settled after
            // another inventory mutation in the same server tick.
            if (!warehouse.consume(supplierOwner, item, filled)) {
                filled = 0;
            }
        }
        if (filled > 0) {
            if (supplierCompany != null) {
                CompanyHelper.recordInventorySale(buyer.getServer(), supplierCompany.companyId(),
                        offer.itemId(), filled, orderSource);
            }
            String destination = TradeRegion.of(buyer.blockPosition());
            deliverOrShip(buyer.getServer(), buyer.getUUID(), item, filled, offer.region(),
                    destination, supplyOrderId, buyerCompanyId, offer.price(), offer.ownerUuid(), initialDeliveryKey);
            if (!buyerCompanyId.isBlank()) {
                CompanyInventoryCostSavedData.get(buyer.getServer()).addInboundOnce(buyerCompanyId, offer.itemId(),
                        filled, EconomyMath.multiply(offer.price(), filled), orderSource);
            }
            SupplyOrderAuditService.record(buyer.getServer(), supplyOrderId,
                    TradeRegion.distance(offer.region(), destination) == 0 ? "DELIVERED" : "DISPATCHED",
                    buyer.getUUID(),
                    offer.ownerUuid(), offer.itemId(), filled, EconomyMath.multiply(offer.price(), filled));
        }
        if (filled > 0) {
            long filledAmount = EconomyMath.multiply(offer.price(), filled);
            if (paySupplier(buyer.getServer(), offer.ownerUuid(), offer.companyName(), filledAmount, initialDeliveryKey)) {
                SupplyEscrowSavedData.get(buyer.getServer()).releaseOnce(supplyOrderId, initialDeliveryKey,
                        Money.toMinorSaturated(filledAmount));
            }
        }

        int remaining = quantity - filled;
        if (remaining > 0) {
            PurchaseOrder backorder = new PurchaseOrder(supplyOrderId, buyer.getUUID(),
                    offer.ownerUuid(), offer.companyName(), offer.itemId(), remaining, offer.region(),
                    TradeRegion.of(buyer.blockPosition()), offer.price(),
                    buyer.getServer().overworld().getGameTime(), buyerCompanyId, offer.qualityScore())
                    .withOriginalQuantity(quantity)
                    .withInputCreditMinor(inputCreditMinor);
            data.replaceOrder(backorder);
            SupplyOrderAuditService.record(buyer.getServer(), supplyOrderId, "BACKORDERED", buyer.getUUID(),
                    offer.ownerUuid(), offer.itemId(), remaining, EconomyMath.multiply(offer.price(), remaining));
        } else {
            SupplyOrderAuditService.record(buyer.getServer(), supplyOrderId, "FULFILLED", buyer.getUUID(),
                    offer.ownerUuid(), offer.itemId(), quantity, total);
            data.removeOrder(supplyOrderId);
        }
        return true;
    }

    /** Recovers prepaid order intents that were interrupted before an order was recorded. */
    public static int recoverPendingOrderIntents(MinecraftServer server) {
        SupplyOrderIntentSavedData intents = SupplyOrderIntentSavedData.get(server);
        SupplyMarketSavedData orders = SupplyMarketSavedData.get(server);
        SupplySettlementSavedData settlements = SupplySettlementSavedData.get(server);
        long now = server.overworld().getGameTime();
        long expiry = PerpetualCalendar.ticksForDays(Config.SUPPLY_ORDER_EXPIRY_DAYS.get());
        int recovered = 0;
        for (SupplyOrderIntentSavedData.Intent intent : new ArrayList<>(intents.intents())) {
            if (orders.findOrder(intent.orderId()) != null) {
                intents.remove(intent.orderId());
                continue;
            }
            long total = EconomyMath.multiply(intent.unitPrice(), intent.quantity());
            long totalMinor = Money.toMinor(total);
            if (total <= 0L || totalMinor <= 0L) continue;
            String source = "supply_order:" + intent.orderId();
            boolean paid = intent.paid();
            if (intent.createdAt() > 0L && expiry > 0L && now >= intent.createdAt()
                    && now - intent.createdAt() >= expiry) {
                paid = paid || hasRecordedOrderPayment(server, intent, source, totalMinor);
                if (paid && !settlements.hasOrderRefund(intent.orderId())) {
                    boolean refundedToCompany = false;
                    if (intent.buyerCompanyId() != null && !intent.buyerCompanyId().isBlank()) {
                        Company company = CompanySavedData.get(server).get(intent.buyerCompanyId());
                        refundedToCompany = company != null && CompanyHelper.creditTreasuryNonOperatingOnce(server,
                                company.companyId(), Currencies.USD.id(), total, "supply_intent_refund",
                                "Expired interrupted supply order refund", "supply-intent-refund:" + intent.orderId());
                    }
                    if (!refundedToCompany) {
                        MarketMailboxSavedData.get(server).creditMoneyOnce(intent.buyerUuid(),
                                Currencies.USD.id(), totalMinor, "supply-intent-refund:" + intent.orderId());
                    }
                    SupplyEscrowSavedData.get(server).refundOnce(intent.orderId(), "intent-expiry:" + intent.orderId(), totalMinor);
                    settlements.recordOrderRefund(intent.orderId());
                    SupplyOrderAuditService.record(server, intent.orderId(),
                            refundedToCompany ? "INTENT_EXPIRED_REFUND_COMPANY" : "INTENT_EXPIRED_REFUND",
                            intent.buyerUuid(), intent.supplierUuid(), intent.itemId(), intent.quantity(), total);
                }
                intents.remove(intent.orderId());
                recovered++;
                continue;
            }
            ServerPlayer buyer = server.getPlayerList().getPlayer(intent.buyerUuid());
            if (buyer == null) continue;
            if (!paid) {
                if (intent.buyerCompanyId() != null && !intent.buyerCompanyId().isBlank()) {
                    Company company = CompanySavedData.get(server).get(intent.buyerCompanyId());
                    paid = company != null && CompanyHelper.debitTreasuryNonOperatingOnce(server,
                            company.companyId(), Currencies.USD.id(), total, "supply_purchase",
                            "采购原料并取得存货", source);
                } else {
                    paid = EconomyHelper.tryPayWithReference(buyer, Currencies.USD, totalMinor, source);
                }
                if (!paid) continue;
                intents.markPaid(intent.orderId());
            }
            FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(server);
            long recoveryTime = server.overworld().getGameTime();
            journal.markCompleted(intent.orderId(), "supply", "buyer-payment", totalMinor, recoveryTime);
            journal.markStarted(intent.orderId(), "supply", "order-record", intent.quantity(), recoveryTime);
            orders.addOrder(new PurchaseOrder(intent.orderId(), intent.buyerUuid(), intent.supplierUuid(),
                    intent.companyName(), intent.itemId(), intent.quantity(), intent.originRegion(),
                    intent.destinationRegion(), intent.unitPrice(), intent.createdAt(), intent.buyerCompanyId(),
                    intent.qualityScore()).withOriginalQuantity(intent.quantity()));
            journal.markCompleted(intent.orderId(), "supply", "order-record", intent.quantity(), recoveryTime);
            journal.markStarted(intent.orderId(), "supply", "buyer-escrow", totalMinor, recoveryTime);
            SupplyEscrowSavedData.get(server).createOnce(intent.orderId(), intent.quantity(), totalMinor);
            journal.markCompleted(intent.orderId(), "supply", "buyer-escrow", totalMinor, recoveryTime);
            intents.remove(intent.orderId());
            recovered++;
            fulfill(server, InventoryOwner.player(intent.supplierUuid()), intent.supplierUuid(), intent.itemId());
        }
        return recovered;
    }

    private static boolean hasRecordedOrderPayment(MinecraftServer server,
                                                    SupplyOrderIntentSavedData.Intent intent,
                                                    String source, long totalMinor) {
        if (intent.buyerCompanyId() != null && !intent.buyerCompanyId().isBlank()) {
            return CompanyHelper.hasNonOperatingDebitSource(server, intent.buyerCompanyId(), source);
        }
        return EconomyLogSavedData.get(server).hasPayment(intent.buyerUuid(), Currencies.USD.id(), totalMinor, source);
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
                        && CompanyHelper.creditTreasuryNonOperatingOnce(server, buyerCompany.companyId(),
                        Currencies.USD.id(), refund, "supply_refund", "Expired undelivered supply order refund",
                        "supply-order-refund:" + order.id());
            }
            if (!refundedToCompany) {
                MarketMailboxSavedData.get(server).creditMoneyOnce(order.buyerUuid(), Currencies.USD.id(),
                        refundMinor, "supply-order-refund:" + order.id());
            }
            SupplyEscrowSavedData.get(server).refundOnce(order.id(), "expiry:" + order.id(), refundMinor);
            long creditToReverse = proportionalInputCredit(order);
            if (creditToReverse > 0L) {
                TaxTransactionService.reverseInputCredit(server, order.buyerUuid(), Currencies.USD.id(),
                        creditToReverse, "supply_order:" + order.id(), now);
            }
            settlements.recordOrderRefund(order.id());
            SupplyOrderAuditService.record(server, order.id(),
                    refundedToCompany ? "EXPIRED_REFUND_COMPANY" : "EXPIRED_REFUND",
                    order.buyerUuid(), order.supplierUuid(), order.itemId(), order.remaining(), refund,
                    "expiry-refund:" + order.id());
            data.removeOrder(order.id());
        }
    }

    /**
     * Returns the prepaid value of a shipment lost in transit. The shipment is
     * still part of the order's remaining quantity, so only that lost batch is
     * refunded; other batches remain eligible for delivery.
     */
    public static void compensateTransportLoss(MinecraftServer server, String shipmentId, String supplyOrderId, UUID buyerUuid,
                                                String buyerCompanyId, UUID supplierUuid, String itemId, int quantity, long unitPrice) {
        if (server == null || shipmentId == null || shipmentId.isBlank() || buyerUuid == null
                || supplyOrderId == null || supplyOrderId.isBlank() || supplierUuid == null || quantity <= 0) return;
        SupplySettlementSavedData settlements = SupplySettlementSavedData.get(server);
        FinancialSettlementJournalSavedData financialJournal = FinancialSettlementJournalSavedData.get(server);
        if (settlements.hasTransportCompensation(shipmentId)) return;

        SupplyMarketSavedData data = SupplyMarketSavedData.get(server);
        PurchaseOrder order = data.orders().stream()
                .filter(candidate -> candidate.id().equals(supplyOrderId))
                .findFirst().orElse(null);
        if (order == null) {
            long refund = EconomyMath.multiply(unitPrice, quantity);
            long refundMinor = refund < 0L ? -1L : Money.toMinor(refund);
            if (refundMinor < 0L) return;
            financialJournal.markStarted(shipmentId, "supply", "loss-refund", refundMinor,
                    server.overworld().getGameTime());
            boolean refundedToCompany = false;
            if (buyerCompanyId != null && !buyerCompanyId.isBlank()) {
                Company company = CompanySavedData.get(server).get(buyerCompanyId);
                refundedToCompany = company != null && CompanyHelper.creditTreasuryNonOperatingOnce(server,
                        company.companyId(), Currencies.USD.id(), refund, "supply_loss_refund",
                        "Transport loss prepaid supply refund", "supply-loss-refund:" + shipmentId);
            }
            if (!refundedToCompany) {
                MarketMailboxSavedData.get(server).creditMoneyOnce(buyerUuid, Currencies.USD.id(), refundMinor,
                        "supply-loss-refund:" + shipmentId);
            }
            financialJournal.markCompleted(shipmentId, "supply", "loss-refund", refundMinor,
                    server.overworld().getGameTime());
            financialJournal.markStarted(shipmentId, "supply", "loss-escrow-refund", refundMinor,
                    server.overworld().getGameTime());
            if (!SupplyEscrowSavedData.get(server).refundOnce(supplyOrderId, "loss:" + shipmentId, refundMinor)) return;
            financialJournal.markCompleted(shipmentId, "supply", "loss-escrow-refund", refundMinor,
                    server.overworld().getGameTime());
            SupplyOrderAuditService.record(server, supplyOrderId, "LOST", buyerUuid, supplierUuid,
                    itemId, quantity, EconomyMath.multiply(unitPrice, quantity), shipmentId);
            settlements.recordTransportCompensation(shipmentId);
            return;
        }

        int lost = Math.min(quantity, Math.max(0, order.remaining()));
        long refund = EconomyMath.multiply(order.unitPrice() > 0L ? order.unitPrice() : unitPrice, lost);
        long refundMinor = refund < 0L ? -1L : Money.toMinor(refund);
        if (lost <= 0 || refundMinor < 0L) return;
        financialJournal.markStarted(shipmentId, "supply", "loss-refund", refundMinor,
                server.overworld().getGameTime());

        boolean refundedToCompany = false;
        if (order.buyerCompanyId() != null && !order.buyerCompanyId().isBlank()) {
            Company company = CompanySavedData.get(server).get(order.buyerCompanyId());
            refundedToCompany = company != null && CompanyHelper.creditTreasuryNonOperatingOnce(server,
                    company.companyId(), Currencies.USD.id(), refund, "supply_loss_refund",
                    "Transport loss prepaid supply refund", "supply-loss-refund:" + shipmentId);
        }
        if (!refundedToCompany) {
            MarketMailboxSavedData.get(server).creditMoneyOnce(buyerUuid, Currencies.USD.id(), refundMinor,
                    "supply-loss-refund:" + shipmentId);
        }
        financialJournal.markCompleted(shipmentId, "supply", "loss-refund", refundMinor,
                server.overworld().getGameTime());
        financialJournal.markStarted(shipmentId, "supply", "loss-escrow-refund", refundMinor,
                server.overworld().getGameTime());
        if (!SupplyEscrowSavedData.get(server).refundOnce(order.id(), "loss:" + shipmentId, refundMinor)) return;
        financialJournal.markCompleted(shipmentId, "supply", "loss-escrow-refund", refundMinor,
                server.overworld().getGameTime());

        long creditToReverse = SupplyOrderTaxCreditCalculator.proportional(order.inputCreditMinor(), lost,
                Math.max(1, order.originalQuantity()));
        if (creditToReverse > 0L) {
            financialJournal.markStarted(shipmentId, "supply", "loss-tax-credit-reversal", creditToReverse,
                    server.overworld().getGameTime());
            TaxTransactionService.reverseInputCredit(server, order.buyerUuid(), Currencies.USD.id(),
                    creditToReverse, "supply_order:" + order.id(), server.overworld().getGameTime());
            financialJournal.markCompleted(shipmentId, "supply", "loss-tax-credit-reversal", creditToReverse,
                    server.overworld().getGameTime());
        }
        int newRemaining = order.remaining() - lost;
        String event = newRemaining <= 0 ? "LOST" : "PARTIAL_LOSS";
        SupplyOrderAuditService.record(server, order, event, lost, refund);
        if (newRemaining <= 0) data.removeOrder(order.id());
        else data.replaceOrder(order.withRemaining(newRemaining));
        settlements.recordTransportCompensation(shipmentId);
    }


    /** Cancels an undelivered backorder and refunds only its remaining escrow. */
    public static boolean cancelOrder(ServerPlayer buyer, String orderId) {
        if (buyer == null || orderId == null || orderId.isBlank()) return false;
        MinecraftServer server = buyer.getServer();
        SupplyMarketSavedData data = SupplyMarketSavedData.get(server);
        PurchaseOrder order = data.orders().stream()
                .filter(candidate -> candidate.id().equals(orderId)
                        && candidate.buyerUuid().equals(buyer.getUUID()))
                .findFirst().orElse(null);
        if (order == null || order.remaining() <= 0 || order.unitPrice() <= 0L) return false;
        SupplySettlementSavedData settlements = SupplySettlementSavedData.get(server);
        if (settlements.hasOrderRefund(order.id())) {
            data.removeOrder(order.id());
            return true;
        }
        long refund = EconomyMath.multiply(order.unitPrice(), order.remaining());
        long refundMinor = refund < 0L ? -1L : Money.toMinor(refund);
        if (refundMinor < 0L) return false;

        boolean refundedToCompany = false;
        if (order.buyerCompanyId() != null && !order.buyerCompanyId().isBlank()) {
            Company company = CompanySavedData.get(server).get(order.buyerCompanyId());
            refundedToCompany = company != null && company.ownerUuid().equals(buyer.getUUID())
                    && CompanyHelper.creditTreasuryNonOperatingOnce(server, company.companyId(),
                    Currencies.USD.id(), refund, "supply_cancel_refund", "Cancelled undelivered supply order refund",
                    "supply-order-refund:" + order.id());
        }
        if (!refundedToCompany) {
            MarketMailboxSavedData.get(server).creditMoneyOnce(buyer.getUUID(), Currencies.USD.id(), refundMinor,
                    "supply-order-refund:" + order.id());
        }
        SupplyEscrowSavedData.get(server).refundOnce(order.id(), "cancel:" + order.id(), refundMinor);
        long creditToReverse = proportionalInputCredit(order);
        if (creditToReverse > 0L) {
            TaxTransactionService.reverseInputCredit(server, buyer.getUUID(), Currencies.USD.id(),
                    creditToReverse, "supply_order:" + order.id(), server.overworld().getGameTime());
        }
        settlements.recordOrderRefund(order.id());
        SupplyOrderAuditService.record(server, order.id(), "CANCELLED_REFUND",
                order.buyerUuid(), order.supplierUuid(), order.itemId(), order.remaining(), refund,
                "cancel-refund:" + order.id());
        data.removeOrder(order.id());
        return true;
    }

    static long proportionalInputCredit(PurchaseOrder order) {
        if (order == null || order.inputCreditMinor() <= 0L || order.remaining() <= 0
                || order.originalQuantity() <= 0) return 0L;
        return SupplyOrderTaxCreditCalculator.proportional(order.inputCreditMinor(),
                order.remaining(), order.originalQuantity());
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
        FinancialSettlementJournalSavedData financialJournal = FinancialSettlementJournalSavedData.get(server);
        for (PurchaseOrder order : new ArrayList<>(data.orders())) {
            if (!order.supplierUuid().equals(supplierUuid) || !order.itemId().equals(itemId)) {
                continue;
            }
            String deliveryKey = order.id() + ":delivery:" + order.remaining();
            SupplyDeliverySavedData.Delivery recorded = deliveryJournal.find(deliveryKey);
            if (recorded != null) {
                // The goods were already dispatched; recover the order state without
                // consuming fresh stock. The payment key makes this retry safe too.
                long recordedAmount = EconomyMath.multiply(order.unitPrice(), recorded.quantity());
                if (!paySupplier(server, order.supplierUuid(), order.companyName(), recordedAmount, deliveryKey)) continue;
                SupplyEscrowSavedData.get(server).releaseOnce(order.id(), deliveryKey,
                        Money.toMinorSaturated(recordedAmount));
                applyDeliveryProgress(server, data, order, recorded.remaining(), recorded.quantity(), false);
                continue;
            }
            Company supplierCompany = CompanyHelper.findCompany(server, supplierUuid, order.companyName());
            InventoryOwner resolvedOwner = supplierCompany == null ? supplierOwner
                    : InventoryOwner.company(supplierCompany.companyId());
            String dispatchPrefix = deliveryKey + ":shipment:";
            int existingDispatch = LogisticsSavedData.get(server).quantityForIdPrefix(dispatchPrefix);
            String localDeliverySource = "supply-local-delivery:" + deliveryKey;
            int existingLocalDelivery = TradeRegion.distance(order.originRegion(), order.destinationRegion()) == 0
                    ? warehouse.creditedQuantity(localDeliverySource) : 0;
            boolean dispatchAlreadyCreated = existingDispatch > 0 || existingLocalDelivery > 0;
            int deliver;
            if (dispatchAlreadyCreated) {
                deliver = Math.min(order.remaining(), Math.max(existingDispatch, existingLocalDelivery));
            } else {
                int stock = warehouse.count(resolvedOwner, itemId);
                if (supplierCompany != null) {
                    stock = CompanyQualityHoldSavedData.get(server)
                            .availableUnits(supplierCompany.companyId(), itemId, stock);
                }
                deliver = Math.min(order.remaining(), stock);
            }
            if (deliver <= 0) continue;
            long settlementTime = server.overworld().getGameTime();
            financialJournal.markStarted(deliveryKey, "supply", "inventory-debit", deliver, settlementTime);
            // Do not create a delivery from a stale stock snapshot. Every
            // downstream side effect is conditional on the actual debit.
            if (!dispatchAlreadyCreated && !warehouse.consume(resolvedOwner, item, deliver)) {
                continue;
            }
            financialJournal.markCompleted(deliveryKey, "supply", "inventory-debit", deliver, settlementTime);
            if (supplierCompany != null) {
                CompanyHelper.recordInventorySale(server, supplierCompany.companyId(),
                        order.itemId(), deliver, order.id() + ":delivery:" + order.remaining());
            }
            String deliveryType = TradeRegion.distance(order.originRegion(), order.destinationRegion()) == 0
                    ? "DELIVERED" : "DISPATCHED";
            financialJournal.markStarted(deliveryKey, "supply", "goods-dispatch", deliver, settlementTime);
            deliverOrShip(server, order.buyerUuid(), item, deliver, order.originRegion(), order.destinationRegion(),
                    order.id(), order.buyerCompanyId(), order.unitPrice(), order.supplierUuid(), deliveryKey);
            financialJournal.markCompleted(deliveryKey, "supply", "goods-dispatch", deliver, settlementTime);
            if (!order.buyerCompanyId().isBlank()) {
                CompanyInventoryCostSavedData.get(server).addInboundOnce(order.buyerCompanyId(), order.itemId(),
                        deliver, EconomyMath.multiply(order.unitPrice(), deliver), deliveryKey);
            }
            int newRemaining = order.remaining() - deliver;
            // Buyer funds for a backorder are held until this portion is
            // dispatched. The undelivered remainder remains refundable.
            long deliveredAmount = EconomyMath.multiply(order.unitPrice(), deliver);
            financialJournal.markStarted(deliveryKey, "supply", "supplier-payout",
                    Money.toMinorSaturated(deliveredAmount), settlementTime);
            if (!paySupplier(server, order.supplierUuid(), order.companyName(), deliveredAmount, deliveryKey)) continue;
            financialJournal.markCompleted(deliveryKey, "supply", "supplier-payout",
                    Money.toMinorSaturated(deliveredAmount), settlementTime);
            financialJournal.markStarted(deliveryKey, "supply", "escrow-release",
                    Money.toMinorSaturated(deliveredAmount), settlementTime);
            SupplyEscrowSavedData.get(server).releaseOnce(order.id(), deliveryKey,
                    Money.toMinorSaturated(deliveredAmount));
            financialJournal.markCompleted(deliveryKey, "supply", "escrow-release",
                    Money.toMinorSaturated(deliveredAmount), settlementTime);
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

    private static boolean paySupplier(MinecraftServer server, UUID supplierUuid, String companyName,
                                    long amount, String sourceId) {
        if (server == null || supplierUuid == null || amount <= 0L) return false;
        SupplySettlementSavedData settlements = SupplySettlementSavedData.get(server);
        if (settlements.hasSupplierPayment(sourceId)) return true;
        long now = server.overworld().getGameTime();
        TaxTransactionService.assess(server, TaxType.VAT, supplierUuid, Currencies.USD.id(),
                Money.toMinorSaturated(amount), "supply-sale:" + sourceId, now);
        Company company = CompanyHelper.findCompany(server, supplierUuid, companyName);
        if (company != null && CompanyHelper.creditTreasuryOnce(server, company.companyId(), Currencies.USD.id(),
                amount, "supply-sale:" + sourceId)) {
            CompanyHelper.recordTaxableIncome(server, company, "supply_sale:" + sourceId,
                    amount, Currencies.USD.id(), now);
            settlements.recordSupplierPayment(sourceId);
            return true;
        }
        ServerPlayer supplier = server.getPlayerList().getPlayer(supplierUuid);
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
        long amountMinor = Money.toMinor(amount);
        if (amountMinor <= 0L) {
            return false;
        }
        String mailboxSource = "supply-supplier-payment:" + sourceId;
        boolean credited = mailbox.hasCreditSource(mailboxSource)
                || mailbox.creditMoneyOnce(supplierUuid, "usd", amountMinor, mailboxSource);
        if (!credited) return false;
        if (supplier != null) mailbox.redeemMoneyOnly(supplier);
        settlements.recordSupplierPayment(sourceId);
        return true;
    }

    private static void deliverOrShip(MinecraftServer server, UUID buyer, Item item, int quantity,
                                      String origin, String destination, String supplyOrderId,
                                      String buyerCompanyId, long unitPrice, UUID supplierUuid,
                                      String dispatchKey) {
        if (quantity <= 0) {
            return;
        }
        if (TradeRegion.distance(origin, destination) == 0) {
            InventoryOwner owner = buyerCompanyId == null || buyerCompanyId.isBlank()
                    ? InventoryOwner.player(buyer) : InventoryOwner.company(buyerCompanyId);
            WarehouseSavedData.get(server).creditOnce(owner, item, quantity,
                    "supply-local-delivery:" + dispatchKey);
            return;
        }
        long distance = TradeRegion.distance(origin, destination);
        TransportMode transport = TransportMode.forDistance(distance);
        LogisticsInfrastructureSavedData infrastructure = LogisticsInfrastructureSavedData.get(server);
        int capacity = transport.capacity() + infrastructure.capacityBonus(origin, destination, transport);
        capacity = com.ailudick.capitalismmod.economy.expansion.EconomicEventService.applyCapacityShock(capacity,
                com.ailudick.capitalismmod.economy.expansion.EconomicEventService.logisticsCapacityShockBps(
                        server, origin, destination, server.overworld().getGameTime()));
        capacity = Math.max(1, capacity);
        long delay;
        try {
            delay = transport.travelTicks(Config.REGIONAL_SHIPPING_TICKS.get(), distance);
            delay = infrastructure.adjustTravelTicks(delay, origin, destination, transport);
            delay = com.ailudick.capitalismmod.market.LogisticsEconomics.congestionDelay(delay, quantity, capacity);
            delay = com.ailudick.capitalismmod.economy.expansion.EconomicEventService.applyTravelShock(delay,
                    com.ailudick.capitalismmod.economy.expansion.EconomicEventService.logisticsCapacityShockBps(
                            server, origin, destination, server.overworld().getGameTime()));
            delay = Math.addExact(server.overworld().getGameTime(), delay);
        } catch (ArithmeticException e) {
            delay = Long.MAX_VALUE;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        LogisticsSavedData data = LogisticsSavedData.get(server);
        int remaining = quantity;
        while (remaining > 0) {
            int batch = Math.min(remaining, capacity);
            int batchIndex = (quantity - remaining) / Math.max(1, capacity);
            String shipmentId = dispatchKey + ":shipment:" + batchIndex;
            data.add(new LogisticsSavedData.Shipment(shipmentId, buyer, itemId, batch, delay,
                    origin, destination, transport, false, 0, supplyOrderId, buyerCompanyId, unitPrice,
                    supplierUuid));
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
