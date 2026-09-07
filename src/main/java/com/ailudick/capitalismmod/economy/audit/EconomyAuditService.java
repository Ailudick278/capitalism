package com.ailudick.capitalismmod.economy.audit;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyLedgerEntry;
import com.ailudick.capitalismmod.company.CompanyLedgerSavedData;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.economy.labor.EmploymentRecord;
import com.ailudick.capitalismmod.economy.labor.LaborMarketSavedData;
import com.ailudick.capitalismmod.economy.labor.LaborPayrollSavedData;
import com.ailudick.capitalismmod.economy.FinancialSettlementJournalSavedData;
import com.ailudick.capitalismmod.economy.EconomicSettlementJournalSavedData;
import com.ailudick.capitalismmod.economy.SettlementJournalRules;
import com.ailudick.capitalismmod.economy.RecoveryIntentRules;
import com.ailudick.capitalismmod.loan.PeerLoanOriginationIntentSavedData;
import com.ailudick.capitalismmod.loan.PeerLoanPaymentSavedData;
import com.ailudick.capitalismmod.bank.BankRecoveryRules;
import com.ailudick.capitalismmod.bank.BankExposureAuditRules;
import com.ailudick.capitalismmod.bank.BankExposureSavedData;
import com.ailudick.capitalismmod.bank.BankCapitalAuditRules;
import com.ailudick.capitalismmod.bank.BankLiquidityAuditRules;
import com.ailudick.capitalismmod.risk.FinancialRiskAuditRules;
import com.ailudick.capitalismmod.economy.contract.ContractDisputeSavedData;
import com.ailudick.capitalismmod.economy.contract.EconomicContractSavedData;
import com.ailudick.capitalismmod.economy.contract.ContractAuditRules;
import com.ailudick.capitalismmod.economy.contract.ContractStatus;
import com.ailudick.capitalismmod.economy.contract.EconomicContractSavedData;
import com.ailudick.capitalismmod.population.Household;
import com.ailudick.capitalismmod.population.PopulationSavedData;
import com.ailudick.capitalismmod.population.HousingLeaseSavedData;
import com.ailudick.capitalismmod.population.PrivateLandlordSavedData;
import com.ailudick.capitalismmod.land.LandLeaseDebtSavedData;
import com.ailudick.capitalismmod.land.LandLeaseSettlementSavedData;
import com.ailudick.capitalismmod.land.LandRentBillSavedData;
import com.ailudick.capitalismmod.supply.SupplyEscrowSavedData;
import com.ailudick.capitalismmod.business.BusinessOrderEscrowSavedData;
import com.ailudick.capitalismmod.business.BusinessOrderSavedData;
import com.ailudick.capitalismmod.auction.AuctionListingIntentSavedData;
import com.ailudick.capitalismmod.market.CommodityBuyIntentSavedData;
import com.ailudick.capitalismmod.market.CommoditySellIntentSavedData;
import com.ailudick.capitalismmod.market.CommodityTradeIntentSavedData;
import com.ailudick.capitalismmod.stock.StockBuyIntentSavedData;
import com.ailudick.capitalismmod.stock.StockSellIntentSavedData;
import com.ailudick.capitalismmod.stock.StockTradeIntentSavedData;
import com.ailudick.capitalismmod.stock.StockOrder;
import com.ailudick.capitalismmod.economy.EconomySavedData;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.market.MarketOrder;
import com.ailudick.capitalismmod.market.LogisticsSavedData;
import com.ailudick.capitalismmod.market.LogisticsDeliverySavedData;
import com.ailudick.capitalismmod.market.LogisticsLossSavedData;
import com.ailudick.capitalismmod.market.LogisticsAuditRules;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.auction.AuctionAuditRules;
import com.ailudick.capitalismmod.auction.AuctionSavedData;
import com.ailudick.capitalismmod.auction.AuctionSettlementSavedData;
import com.ailudick.capitalismmod.auction.AuctionListingIntentSavedData;
import com.ailudick.capitalismmod.auction.AuctionBidSavedData;
import com.ailudick.capitalismmod.auction.AuctionSettlementAuditRules;
import com.ailudick.capitalismmod.supply.SupplySettlementAuditRules;
import com.ailudick.capitalismmod.supply.SupplyEscrowAuditRules;
import com.ailudick.capitalismmod.supply.SupplyOrderAuditSavedData;
import com.ailudick.capitalismmod.supply.SupplyOrderAuditRules;
import com.ailudick.capitalismmod.business.BusinessEscrowAuditRules;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.currency.Currencies;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import com.ailudick.capitalismmod.supply.SupplyOrderIntentSavedData;
import com.ailudick.capitalismmod.bank.BankCashDepositIntentSavedData;
import com.ailudick.capitalismmod.bank.BankRepaymentIntentSavedData;
import com.ailudick.capitalismmod.bank.BankTransactionAuditRules;
import com.ailudick.capitalismmod.init.ModAttachments;
import com.ailudick.capitalismmod.currency.CurrencyExchangeIntentSavedData;
import com.ailudick.capitalismmod.economy.PlayerTransferIntentSavedData;
import com.ailudick.capitalismmod.government.GovernmentPolicySavedData;
import com.ailudick.capitalismmod.bond.BondSavedData;
import com.ailudick.capitalismmod.population.HouseholdConsumptionSavedData;
import com.ailudick.capitalismmod.bank.BankExposureSavedData;
import com.ailudick.capitalismmod.risk.FinancialRiskSavedData;
import com.ailudick.capitalismmod.bank.BankLiquiditySavedData;
import com.ailudick.capitalismmod.bank.BankCapitalSavedData;
import com.ailudick.capitalismmod.tax.TaxIncomeVoucherLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxIncomeVoucherAuditRules;
import com.ailudick.capitalismmod.tax.TaxInvoiceSavedData;
import com.ailudick.capitalismmod.tax.TaxInvoiceAuditRules;
import com.ailudick.capitalismmod.tax.TaxInvoiceLinkRules;
import com.ailudick.capitalismmod.tax.TaxLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxBill;
import com.ailudick.capitalismmod.tax.TaxCreditSavedData;
import com.ailudick.capitalismmod.tax.TaxCreditAuditRules;
import com.ailudick.capitalismmod.tax.TaxPaymentAuditRules;
import com.ailudick.capitalismmod.economy.EconomyLogSavedData;
import com.ailudick.capitalismmod.tax.TaxPayment;
import com.ailudick.capitalismmod.government.TaxRevenueAuditRules;
import com.ailudick.capitalismmod.company.CompanyLifecycleService;
import com.ailudick.capitalismmod.company.CompanyFreightContractSavedData;
import com.ailudick.capitalismmod.company.FreightContractAuditRules;
import com.ailudick.capitalismmod.company.CompanyLogisticsCostSavedData;
import com.ailudick.capitalismmod.company.CompanyFreightSettlementSavedData;
import com.ailudick.capitalismmod.company.FreightSettlementAuditRules;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Read-only invariant checks for persisted economic ledgers. */
public final class EconomyAuditService {
    private EconomyAuditService() {}

    public static List<String> audit(MinecraftServer server) {
        List<String> issues = new ArrayList<>();
        FinancialSettlementJournalSavedData financialJournal = FinancialSettlementJournalSavedData.get(server);
        financialJournal.pendingEntries().stream().limit(100)
                .forEach(entry -> issues.add("pending financial settlement "
                        + entry.transactionId() + "/" + entry.instrument() + "/" + entry.phase()));
        EconomicSettlementJournalSavedData.get(server).pendingEntries().stream().limit(100)
                .forEach(entry -> issues.add("pending daily settlement "
                        + entry.day() + "/" + entry.phase()));
        for (var entry : financialJournal.entries()) {
            if (!SettlementJournalRules.validFinancial(entry.transactionId(), entry.instrument(), entry.phase(),
                    entry.status(), entry.amountMinor(), entry.gameTime())) {
                issues.add("financial settlement journal invalid " + entry.transactionId() + "/" + entry.phase());
            }
        }
        for (var entry : EconomicSettlementJournalSavedData.get(server).entries()) {
            if (!SettlementJournalRules.validDaily(entry.day(), entry.phase(), entry.status(), entry.gameTime())) {
                issues.add("daily settlement journal invalid " + entry.day() + "/" + entry.phase());
            }
        }
        AuctionListingIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending auction listing "
                        + intent.auctionId() + "/" + intent.itemId() + "/" + intent.escrowed()));
        CommodityBuyIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending commodity buy " + intent.orderId()));
        CommoditySellIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending commodity sell " + intent.orderId()));
        CommodityTradeIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending commodity trade " + intent.id()));
        StockBuyIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending stock buy " + intent.orderId()));
        StockSellIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending stock sell " + intent.orderId()));
        StockTradeIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending stock trade " + intent.id()));
        CommoditySavedData commodityBook = CommoditySavedData.get(server);
        for (var intent : CommodityTradeIntentSavedData.get(server).intents()) {
            var buy = commodityBook.findOrder(intent.buyOrderId());
            var sell = commodityBook.findOrder(intent.sellOrderId());
            if (intent.fill() <= 0 || intent.gross() < 0L || intent.buyQuantityBefore() < intent.fill()
                    || intent.sellQuantityBefore() < intent.fill()
                    || !reconciledOrderQuantity(buy, intent.buyQuantityBefore(), intent.fill())
                    || !reconciledOrderQuantity(sell, intent.sellQuantityBefore(), intent.fill())) {
                issues.add("commodity trade intent quantity mismatch " + intent.id());
            }
            if (buy != null && (buy.sell() || !buy.ownerId().equals(intent.buyer().toString())
                    || !Commodities.id(buy.commodity()).equals(intent.itemId()))) {
                issues.add("commodity trade intent buy order mismatch " + intent.id());
            }
            if (sell != null && (!sell.sell() || !sell.ownerId().equals(intent.seller().toString())
                    || !Commodities.id(sell.commodity()).equals(intent.itemId()))) {
                issues.add("commodity trade intent sell order mismatch " + intent.id());
            }
        }
        EconomySavedData stockBook = EconomySavedData.get(server);
        for (var intent : StockTradeIntentSavedData.get(server).intents()) {
            var buy = stockBook.findOrder(intent.buyOrderId());
            var sell = stockBook.findOrder(intent.sellOrderId());
            if (intent.fill() <= 0 || intent.gross() < 0L || intent.buyQuantityBefore() < intent.fill()
                    || intent.sellQuantityBefore() < intent.fill()
                    || !reconciledOrderQuantity(buy, intent.buyQuantityBefore(), intent.fill())
                    || !reconciledOrderQuantity(sell, intent.sellQuantityBefore(), intent.fill())) {
                issues.add("stock trade intent quantity mismatch " + intent.id());
            }
            if (buy != null && (buy.sell() || !buy.ownerId().equals(intent.buyer().toString())
                    || !buy.stockId().equals(intent.stockId()))) {
                issues.add("stock trade intent buy order mismatch " + intent.id());
            }
            if (sell != null && (!sell.sell() || !sell.ownerId().equals(intent.seller().toString())
                    || !sell.stockId().equals(intent.stockId()))) {
                issues.add("stock trade intent sell order mismatch " + intent.id());
            }
        }
        Set<String> marketOrderIds = new HashSet<>();
        CommoditySavedData commodities = CommoditySavedData.get(server);
        for (MarketOrder order : commodities.orders()) {
            if (order.id().isBlank() || !marketOrderIds.add("commodity:" + order.id())
                    || !isUuid(order.ownerId()) || order.commodity().isEmpty()
                    || com.ailudick.capitalismmod.market.Commodities.byId(
                    com.ailudick.capitalismmod.market.Commodities.id(order.commodity())) == null
                    || order.quantity() <= 0 || order.pricePerUnit() <= 0L || order.createdAt() < 0L) {
                issues.add("commodity order invalid " + order.id());
            }
        }
        EconomySavedData stocks = EconomySavedData.get(server);
        for (StockOrder order : stocks.orders()) {
            if (order.id().isBlank() || !marketOrderIds.add("stock:" + order.id())
                    || !isUuid(order.ownerId()) || order.stockId().isBlank() || !stocks.isStock(order.stockId())
                    || order.quantity() <= 0 || order.pricePerUnit() <= 0L || order.createdAt() < 0L) {
                issues.add("stock order invalid " + order.id());
            }
        }
        Set<String> shipmentIds = new HashSet<>();
        for (var shipment : LogisticsSavedData.get(server).shipments()) {
            if (!LogisticsAuditRules.validShipment(shipment.id(), shipment.buyer(), shipment.itemId(),
                    shipment.quantity(), shipment.deliveryTick(), shipment.transport(),
                    shipment.disruptionCount(), shipment.unitPrice())
                    || !LogisticsAuditRules.claimUnique(shipmentIds, shipment.id())) {
                issues.add("logistics shipment invalid " + shipment.id());
            }
        }
        for (var delivery : LogisticsDeliverySavedData.get(server).deliveries()) {
            if (!LogisticsAuditRules.validDelivery(delivery.shipmentId(), delivery.buyer(), delivery.itemId(),
                    delivery.quantity(), delivery.deliveredAt())
                    || !LogisticsAuditRules.claimUnique(shipmentIds, delivery.shipmentId())) {
                issues.add("logistics delivery invalid " + delivery.shipmentId());
            }
        }
        for (var loss : LogisticsLossSavedData.get(server).losses()) {
            if (!LogisticsAuditRules.validLoss(loss.shipmentId(), loss.buyer(), loss.itemId(), loss.quantity(),
                    loss.transport(), loss.disruptionCount(), loss.lostAt(), loss.unitPrice())
                    || !LogisticsAuditRules.claimUnique(shipmentIds, loss.shipmentId())) {
                issues.add("logistics loss invalid " + loss.shipmentId());
            }
        }
        Set<String> freightIds = new HashSet<>();
        Set<String> openFreightShipments = new HashSet<>();
        Set<String> deliveredShipments = LogisticsDeliverySavedData.get(server).deliveries().stream()
                .map(LogisticsDeliverySavedData.Delivery::shipmentId).collect(java.util.stream.Collectors.toSet());
        Set<String> lostShipments = LogisticsLossSavedData.get(server).losses().stream()
                .map(LogisticsLossSavedData.Loss::shipmentId).collect(java.util.stream.Collectors.toSet());
        for (var freight : CompanyFreightContractSavedData.get(server).contracts()) {
            if (!freightIds.add(freight.id())
                    || !FreightContractAuditRules.valid(freight.id(), freight.shipmentId(),
                    freight.buyerCompanyId(), freight.carrierCompanyId(), freight.quotedCost(), freight.createdAt(),
                    freight.acceptedAt(), freight.expiresAt(), freight.status())) {
                issues.add("freight contract invalid " + freight.id());
            }
            if (("offered".equals(freight.status()) || "accepted".equals(freight.status()))
                    && !openFreightShipments.add(freight.shipmentId())) {
                issues.add("multiple open freight contracts " + freight.shipmentId());
            }
            if (!FreightContractAuditRules.terminalEvidence(freight.status(),
                    deliveredShipments.contains(freight.shipmentId()), lostShipments.contains(freight.shipmentId()))) {
                issues.add("freight contract terminal evidence mismatch " + freight.id());
            }
        }
        Set<String> freightCostIds = new HashSet<>();
        CompanyLogisticsCostSavedData freightCosts = CompanyLogisticsCostSavedData.get(server);
        for (var cost : freightCosts.costs()) {
            if (!freightCostIds.add(cost.shipmentId())
                    || !FreightSettlementAuditRules.validCost(cost.shipmentId(), cost.companyId(), cost.itemId(),
                    cost.quantity(), cost.estimatedCost(), cost.appliedAt(), cost.settled(),
                    cost.carrierCompanyId(), cost.settledAt())) {
                issues.add("freight payable invalid " + cost.shipmentId());
            }
            if (CompanySavedData.get(server).get(cost.companyId()) == null) {
                issues.add("freight payable has no buyer company " + cost.shipmentId());
            }
            var contract = CompanyFreightContractSavedData.get(server).findByShipment(cost.shipmentId());
            if (contract != null && !FreightSettlementAuditRules.quoteMatchesCost(contract.quotedCost(), cost.estimatedCost())) {
                issues.add("freight quote differs from payable " + cost.shipmentId());
            }
        }
        for (var settlement : CompanyFreightSettlementSavedData.get(server).settlements()) {
            if (!FreightSettlementAuditRules.validSettlement(settlement.shipmentId(), settlement.buyerCompanyId(),
                    settlement.carrierCompanyId(), settlement.amount(), settlement.buyerDebited(),
                    settlement.carrierCredited(), settlement.payableClosed())) {
                issues.add("freight settlement phase invalid " + settlement.shipmentId());
                continue;
            }
            var cost = freightCosts.costs().stream()
                    .filter(value -> settlement.shipmentId().equals(value.shipmentId())).findFirst().orElse(null);
            if (cost == null || cost.estimatedCost() != settlement.amount()
                    || (cost.settled() && !settlement.payableClosed())) {
                issues.add("freight settlement amount mismatch " + settlement.shipmentId());
            }
        }
        Set<String> supplyEventKeys = new HashSet<>();
        Map<String, SupplyOrderAuditSavedData.Event> supplyIdentities = new HashMap<>();
        Map<String, Long> supplyOrdered = new HashMap<>();
        Map<String, Long> supplyDelivered = new HashMap<>();
        Map<String, Long> supplyDispatched = new HashMap<>();
        Map<String, Integer> supplyCreated = new HashMap<>();
        for (var event : SupplyOrderAuditSavedData.get(server).events()) {
            if (!SupplyOrderAuditRules.validEvent(event.orderId(), event.type(), event.buyerUuid(),
                    event.supplierUuid(), event.itemId(), event.quantity(), event.amount(), event.occurredAt(),
                    validItemId(event.itemId()))) {
                issues.add("supply order audit event invalid " + event.orderId());
                continue;
            }
            if (!event.eventKey().isBlank() && !supplyEventKeys.add(event.orderId() + ":" + event.type() + ":" + event.eventKey())) {
                issues.add("duplicate supply order audit event " + event.orderId() + "/" + event.type());
            }
            SupplyOrderAuditSavedData.Event identity = supplyIdentities.putIfAbsent(event.orderId(), event);
            if (identity != null && !SupplyOrderAuditRules.sameOrderIdentity(event.buyerUuid(), event.supplierUuid(),
                    event.itemId(), identity.buyerUuid(), identity.supplierUuid(), identity.itemId())) {
                issues.add("supply order audit identity mismatch " + event.orderId());
            }
            if ("CREATED".equals(event.type())) {
                supplyCreated.merge(event.orderId(), 1, Integer::sum);
                supplyOrdered.merge(event.orderId(), (long) event.quantity(), Math::max);
            } else if ("DELIVERED".equals(event.type())) {
                supplyDelivered.merge(event.orderId(), (long) event.quantity(), EconomyAuditService::safeAdd);
            } else if ("DISPATCHED".equals(event.type())) {
                supplyDispatched.merge(event.orderId(), (long) event.quantity(), EconomyAuditService::safeAdd);
            }
        }
        for (String orderId : supplyIdentities.keySet()) {
            long ordered = supplyOrdered.getOrDefault(orderId, 0L);
            if (supplyCreated.getOrDefault(orderId, 0) != 1
                    || !SupplyOrderAuditRules.deliveryTotalsWithinOrder(ordered,
                    supplyDelivered.getOrDefault(orderId, 0L), supplyDispatched.getOrDefault(orderId, 0L))) {
                issues.add("supply order audit quantity mismatch " + orderId);
            }
        }
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
        auditMailboxMoney(mailbox.moneyBalances(), "market mailbox money", issues);
        auditMailboxMoney(mailbox.transferBalances(), "market mailbox transfer", issues);
        for (var player : mailbox.itemBalances().entrySet()) {
            if (player.getKey() == null) issues.add("market mailbox item has no player");
            for (var item : player.getValue().entrySet()) {
                if (!validItemId(item.getKey()) || item.getValue() == null || item.getValue() <= 0) {
                    issues.add("market mailbox item invalid " + item.getKey());
                }
            }
        }
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        for (var owner : warehouse.allStorage().entrySet()) {
            if (owner.getKey() == null || owner.getKey().isBlank()) issues.add("warehouse owner invalid");
            for (var item : owner.getValue().entrySet()) {
                if (!validItemId(item.getKey()) || item.getValue() == null || item.getValue() <= 0) {
                    issues.add("warehouse stock invalid " + owner.getKey() + "/" + item.getKey());
                }
            }
        }
        for (var consumed : warehouse.consumedSourceItems().entrySet()) {
            Integer quantity = warehouse.consumedSourceQuantities().get(consumed.getKey());
            if (!validItemId(consumed.getValue()) || quantity == null || quantity <= 0) {
                issues.add("warehouse consumed source invalid " + consumed.getKey());
            }
        }
        for (var consumed : warehouse.consumedSourceQuantities().entrySet()) {
            if (!warehouse.consumedSourceItems().containsKey(consumed.getKey()) || consumed.getValue() == null
                    || consumed.getValue() <= 0) {
                issues.add("warehouse consumed quantity missing item " + consumed.getKey());
            }
        }
        for (var batch : warehouse.consumedSourceBatches().entrySet()) {
            if (batch.getKey() == null || batch.getKey().isBlank() || batch.getValue() == null
                    || batch.getValue().isBlank() || !warehouse.hasConsumedSource(batch.getKey())) {
                issues.add("warehouse consumed batch invalid " + batch.getKey());
            }
        }
        SupplyOrderIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending supply order " + intent.orderId()));
        BankCashDepositIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending bank cash deposit " + intent.source()));
        BankRepaymentIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending bank repayment " + intent.id()));
        CurrencyExchangeIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending currency exchange " + intent.id()));
        PlayerTransferIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending player transfer " + intent.id()));
        Set<String> recoveryIds = new HashSet<>();
        for (var intent : CurrencyExchangeIntentSavedData.get(server).intents()) {
            if (!recoveryIds.add("exchange:" + intent.id())
                    || !RecoveryIntentRules.validExchange(intent.id(), intent.player(), intent.from(), intent.to(),
                    intent.amount(), intent.converted(), Currencies.exists(intent.from()), Currencies.exists(intent.to()))) {
                issues.add("currency exchange intent invalid " + intent.id());
            }
        }
        for (var intent : PlayerTransferIntentSavedData.get(server).intents()) {
            if (!recoveryIds.add("transfer:" + intent.id())
                    || !RecoveryIntentRules.validTransfer(intent.id(), intent.sender(), intent.target(),
                    intent.currencyId(), intent.amount(), Currencies.exists(intent.currencyId()))) {
                issues.add("player transfer intent invalid " + intent.id());
            }
        }
        for (var intent : PeerLoanOriginationIntentSavedData.get(server).intents()) {
            if (!recoveryIds.add("loan:" + intent.loanId())
                    || !RecoveryIntentRules.validOrigination(intent.loanId(), intent.lender(), intent.borrower(),
                    intent.currencyId(), intent.principal(), intent.ratePerYear(), intent.days(),
                    Currencies.exists(intent.currencyId()))) {
                issues.add("peer loan origination invalid " + intent.loanId());
            }
        }
        Set<String> paymentKeys = new HashSet<>();
        for (var payment : PeerLoanPaymentSavedData.get(server).forAll()) {
            String paymentKey = PeerLoanPaymentSavedData.payoutSource(payment);
            if (!paymentKeys.add(paymentKey)
                    || !RecoveryIntentRules.validPayment(payment.loanId(), payment.lender(), payment.borrower(),
                    payment.currencyId(), payment.timestamp(), payment.total(), payment.interest(),
                    payment.principal(), payment.remainingPrincipal(), payment.daysRemaining(),
                    Currencies.exists(payment.currencyId()))) {
                issues.add("peer loan payment invalid " + payment.loanId());
            }
        }
        Set<String> bankIntentIds = new HashSet<>();
        for (var intent : BankCashDepositIntentSavedData.get(server).intents()) {
            if (!bankIntentIds.add("deposit:" + intent.source())
                    || !BankRecoveryRules.validCashDeposit(intent.source(), intent.playerUuid(), intent.accountId(),
                    intent.currencyId(), intent.amount(), intent.physicalBefore(), Currencies.exists(intent.currencyId()))) {
                issues.add("bank cash deposit intent invalid " + intent.source());
            }
        }
        for (var intent : BankRepaymentIntentSavedData.get(server).intents()) {
            if (!bankIntentIds.add("repayment:" + intent.id())
                    || !BankRecoveryRules.validRepayment(intent.id(), intent.player(), intent.accountId(),
                    intent.currencyId(), intent.debtBefore(), intent.amount(), Currencies.exists(intent.currencyId()))) {
                issues.add("bank repayment intent invalid " + intent.id());
            }
        }
        Set<String> auctionIds = new HashSet<>();
        AuctionSavedData auctions = AuctionSavedData.get(server);
        for (var auction : auctions.auctions()) {
            if (!auctionIds.add(auction.id())
                    || !AuctionAuditRules.validAuction(auction.id(), auction.seller(), auction.itemId(),
                    auction.quantity(), auction.startingPrice(), auction.currentBid(),
                    auction.currentBidder(), auction.endTick())
                    || !validItemId(auction.itemId())) {
                issues.add("auction invalid " + auction.id());
            }
            if (AuctionSettlementSavedData.get(server).has(auction.id())) {
                issues.add("settled auction remains active " + auction.id());
            }
        }
        for (String settledId : AuctionSettlementSavedData.get(server).settledIds()) {
            if (settledId == null || settledId.isBlank()) issues.add("auction settlement id invalid");
        }
        Set<String> bidKeys = new HashSet<>();
        for (var bid : AuctionBidSavedData.get(server).bids()) {
            if (bid.auctionId().isBlank() || bid.bidder() == null || bid.amount() <= 0L
                    || !bidKeys.add(bid.auctionId() + ":" + bid.bidder() + ":" + bid.amount())) {
                issues.add("auction bid invalid " + bid.auctionId());
            }
            var auction = auctions.findAuction(bid.auctionId());
            if (auction != null && (auction.seller().equals(bid.bidder()) || bid.amount() > auction.currentBid())) {
                issues.add("auction bid exceeds current record " + bid.auctionId());
            }
        }
        Set<String> listingIntentIds = new HashSet<>();
        for (var intent : AuctionListingIntentSavedData.get(server).intents()) {
            if (!listingIntentIds.add(intent.auctionId())
                    || !AuctionAuditRules.validListingIntent(intent.auctionId(), intent.seller(), intent.itemId(),
                    intent.quantity(), intent.startingPrice(), intent.endTick(), intent.warehouseBefore())
                    || !validItemId(intent.itemId())) {
                issues.add("auction listing intent invalid " + intent.auctionId());
            }
        }
        Map<String, Set<String>> auctionJournalPhases = new HashMap<>();
        for (var entry : financialJournal.entries()) {
            if (!"auction".equals(entry.instrument()) || !entry.transactionId().startsWith("auction:")
                    || !"completed".equals(entry.status())) continue;
            auctionJournalPhases.computeIfAbsent(entry.transactionId().substring("auction:".length()),
                    ignored -> new HashSet<>()).add(entry.phase());
        }
        for (var entry : auctionJournalPhases.entrySet()) {
            String auctionId = entry.getKey();
            boolean active = auctions.findAuction(auctionId) != null;
            boolean settled = AuctionSettlementSavedData.get(server).has(auctionId);
            if (!active && !settled) {
                issues.add("auction journal has no active or settled record " + auctionId);
            } else if (settled && !AuctionSettlementAuditRules.hasCompleteOutcome(entry.getValue())) {
                issues.add("settled auction journal missing terminal phase " + auctionId);
            }
        }
        Map<String, Set<String>> supplyJournalPhases = new HashMap<>();
        for (var entry : financialJournal.entries()) {
            if (!"supply".equals(entry.instrument()) || !"completed".equals(entry.status())) continue;
            supplyJournalPhases.computeIfAbsent(entry.transactionId(), ignored -> new HashSet<>()).add(entry.phase());
        }
        for (var entry : supplyJournalPhases.entrySet()) {
            String transactionId = entry.getKey();
            if (transactionId.contains(":delivery:") && !SupplySettlementAuditRules.hasCompleteDelivery(entry.getValue())) {
                issues.add("supply delivery journal missing terminal phase " + transactionId);
            }
            if (entry.getValue().contains("loss-refund")
                    && !SupplySettlementAuditRules.hasCompleteLossRefund(entry.getValue())) {
                issues.add("supply loss journal missing escrow refund " + transactionId);
            }
        }
        GovernmentPolicySavedData government = GovernmentPolicySavedData.get(server);
        if (government.treasuryMinor() < 0L) issues.add("government treasury is negative");
        for (var transaction : government.transactions()) {
            if (transaction.id().isBlank() || transaction.amount() <= 0L || transaction.balanceAfter() < 0L) {
                issues.add("government spending transaction invalid " + transaction.id());
            }
        }
        for (var revenue : government.taxRevenues()) {
            if (revenue.id().isBlank() || revenue.originalAmount() <= 0L
                    || revenue.convertedAmount() <= 0L || revenue.balanceAfter() < 0L) {
                issues.add("government tax revenue invalid " + revenue.id());
            }
            String prefix = "tax-payment:";
            TaxPayment payment = revenue.id() != null && revenue.id().startsWith(prefix)
                    ? taxLedgerPayment(TaxLedgerSavedData.get(server).payments(), revenue.id().substring(prefix.length()))
                    : null;
            TaxBill bill = payment == null ? null : TaxLedgerSavedData.get(server).get(payment.billId());
            long converted = payment == null || !Currencies.exists(payment.currencyId()) ? 0L
                    : ExchangeRates.convert(payment.amount(), Currencies.byId(payment.currencyId()), Config.defaultCurrency());
            if (!TaxRevenueAuditRules.matches(revenue, payment, bill, converted)) {
                issues.add("government tax revenue has no matching payment " + revenue.id());
            }
        }
        for (var holding : BondSavedData.get(server).holdings()) {
            if (holding.id().isBlank() || holding.holder() == null || holding.faceValue() <= 0L
                    || holding.ratePerYear() < 0.0 || holding.totalDays() <= 0
                    || holding.daysToMaturity() < 0 || holding.daysToMaturity() > holding.totalDays()) {
                issues.add("bond holding invalid " + holding.id());
            }
        }
        for (var entry : BankExposureSavedData.get(server).exposures().entrySet()) {
            var exposure = entry.getValue();
            if (entry.getKey() == null || exposure.depositsMinor() < 0L || exposure.loanDebtMinor() < 0L
                    || exposure.overdueDebtMinor() < 0L || exposure.overdueDebtMinor() > exposure.loanDebtMinor()
                    || exposure.overdueAccounts() < 0 || exposure.syncedAt() < 0L) {
                issues.add("bank exposure invalid " + entry.getKey());
            }
        }
        BankExposureSavedData exposureData = BankExposureSavedData.get(server);
        for (var player : server.getPlayerList().getPlayers()) {
            if (!BankExposureAuditRules.matches(exposureData.expected(player), exposureData.exposure(player.getUUID()))) {
                issues.add("online bank exposure differs from account state " + player.getUUID());
            }
            for (var accountEntry : player.getData(ModAttachments.BANK_ACCOUNTS).entrySet()) {
                var account = accountEntry.getValue();
                if (account == null || !accountEntry.getKey().equals(account.id())
                        || !BankTransactionAuditRules.validAccount(account.id(), account.credit(), account.balances(),
                        account.debts(), account.loanDaysRemaining())) {
                    issues.add("bank account snapshot invalid " + player.getUUID() + "/" + accountEntry.getKey());
                    continue;
                }
                Set<String> transactionReferences = new HashSet<>();
                for (var transaction : account.transactions()) {
                    if (!BankTransactionAuditRules.validTransaction(transaction.type(), transaction.currencyId(),
                            transaction.amount(), transaction.occurredAt(), transaction.reference(),
                            Currencies.exists(transaction.currencyId()))) {
                        issues.add("bank transaction invalid " + account.id());
                    }
                    if ("repay".equals(transaction.type()) && transaction.reference().startsWith("bank-repayment:")
                            && !EconomyLogSavedData.get(server).hasPayment(player.getUUID(), transaction.currencyId(),
                            -transaction.amount(), transaction.reference())) {
                        issues.add("bank repayment has no wallet evidence " + account.id());
                    }
                    if (!transaction.reference().isBlank() && !transactionReferences.add(transaction.reference())) {
                        issues.add("duplicate bank transaction reference " + account.id());
                    }
                }
            }
        }
        for (var snapshot : FinancialRiskSavedData.get(server).snapshots()) {
            if (!FinancialRiskAuditRules.validDerivedSnapshot(snapshot)) {
                issues.add("financial risk snapshot invalid day " + snapshot.day());
            }
        }
        var liquiditySnapshots = BankLiquiditySavedData.get(server).snapshots();
        if (!BankLiquidityAuditRules.validHistory(liquiditySnapshots)) {
            issues.add("bank liquidity snapshot history invalid");
        }
        BankCapitalSavedData bankCapital = BankCapitalSavedData.get(server);
        if (!BankCapitalAuditRules.validState(bankCapital.initialized(), bankCapital.capitalMinor(),
                bankCapital.cumulativeProfitLossMinor(), bankCapital.lossProvisionMinor(),
                bankCapital.lastSettlementDay())) {
            issues.add("bank capital ledger invalid");
        }
        long bankOverdue = 0L;
        for (var entry : exposureData.exposures().values()) {
            bankOverdue = safeAdd(bankOverdue, entry.overdueDebtMinor());
        }
        long currentDay = server.overworld().getGameTime() / 24_000L;
        if (!BankCapitalAuditRules.provisionMatchesAfterClose(bankCapital.lossProvisionMinor(), bankOverdue,
                bankCapital.lastSettlementDay(), currentDay)) {
            issues.add("bank loss provision differs from settled overdue exposure");
        }
        CompanySavedData companies = CompanySavedData.get(server);
        CompanyLedgerSavedData ledgers = CompanyLedgerSavedData.get(server);
        for (Company company : companies.companies().values()) {
            company.treasury().forEach((currency, balance) -> { if (balance == null || balance < 0L) issues.add("company " + company.companyId() + " negative treasury " + currency); });
            Map<String, Long> previous = new HashMap<>();
            for (CompanyLedgerEntry entry : ledgers.entries(company.companyId())) {
                if (entry.amount() == Long.MIN_VALUE || entry.balanceAfter() < 0L) issues.add("company " + company.companyId() + " invalid ledger entry at " + entry.timestamp());
                Long old = previous.get(entry.currencyId());
                if (old != null && safeAdd(old, entry.amount()) != entry.balanceAfter()) issues.add("company " + company.companyId() + " broken balance chain " + entry.currencyId() + " at " + entry.timestamp());
                previous.put(entry.currencyId(), entry.balanceAfter());
            }
        }
        Set<String> incomeVoucherSources = new HashSet<>();
        for (var voucher : TaxIncomeVoucherLedgerSavedData.get(server).all()) {
            Company company = companies.get(voucher.subjectId());
            if (!incomeVoucherSources.add(voucher.sourceId())
                    || !TaxIncomeVoucherAuditRules.valid(voucher.taxpayerUuid(), voucher.subjectId(),
                    voucher.category(), voucher.currencyId(), voucher.amount(), voucher.occurredAt(),
                    voucher.sourceId(), Currencies.exists(voucher.currencyId()))) {
                issues.add("income voucher invalid " + voucher.sourceId());
            }
            if (company == null) {
                issues.add("income voucher has no company " + voucher.subjectId());
            } else if (!company.ownerUuid().equals(voucher.taxpayerUuid())) {
                issues.add("income voucher owner mismatch " + voucher.sourceId());
            } else {
                boolean ledgerEvidence = ledgers.entries(company.companyId()).stream().anyMatch(entry ->
                        entry.amount() == voucher.amount() && voucher.currencyId().equals(entry.currencyId())
                                && entry.timestamp() == voucher.occurredAt());
                if (!ledgerEvidence) issues.add("income voucher has no ledger evidence " + voucher.sourceId());
            }
        }
        Set<String> invoiceIds = new HashSet<>();
        Set<String> invoiceSources = new HashSet<>();
        for (var invoice : TaxInvoiceSavedData.get(server).invoices()) {
            if (!invoiceIds.add(invoice.id()) || !invoiceSources.add(invoice.sourceEventId())
                    || !TaxInvoiceAuditRules.valid(invoice.id(), invoice.sourceEventId(), invoice.taxpayerUuid(),
                    invoice.currencyId(), invoice.grossAmount(), invoice.taxAmount(), invoice.creditApplied(),
                    invoice.issuedAt(), invoice.direction(), Currencies.exists(invoice.currencyId()))) {
                issues.add("tax invoice invalid " + invoice.sourceEventId());
            }
            TaxBill bill = TaxLedgerSavedData.get(server).findBySourceEvent(invoice.sourceEventId());
            if (!TaxInvoiceLinkRules.matchesBill(invoice.direction(), invoice.grossAmount(), invoice.taxAmount(),
                    invoice.creditApplied(), invoice.taxpayerUuid(), invoice.currencyId(), invoice.sourceEventId(), bill)) {
                issues.add("tax invoice bill mismatch " + invoice.sourceEventId());
            }
        }
        TaxLedgerSavedData taxLedger = TaxLedgerSavedData.get(server);
        Set<String> paymentIds = new HashSet<>();
        Map<String, Long> paymentTotals = new HashMap<>();
        for (var payment : taxLedger.payments()) {
            TaxBill bill = taxLedger.get(payment.billId());
            if (!paymentIds.add(payment.id())
                    || !TaxPaymentAuditRules.valid(payment.id(), payment.billId(), payment.taxpayerUuid(),
                    payment.currencyId(), payment.amount(), payment.paidAt(), Currencies.exists(payment.currencyId()))
                    || !TaxPaymentAuditRules.matchesBill(payment, bill)) {
                issues.add("tax payment invalid " + payment.id());
                continue;
            }
            long total = safeAdd(paymentTotals.getOrDefault(payment.billId(), 0L), payment.amount());
            paymentTotals.put(payment.billId(), total);
            if (!TaxPaymentAuditRules.totalWithinDue(total, bill.totalDue())) {
                issues.add("tax payment exceeds bill " + payment.billId());
            }
            if (payment.settlementReference().startsWith("tax-payment:")
                    && !EconomyLogSavedData.get(server).hasPayment(payment.taxpayerUuid(), payment.currencyId(),
                    payment.amount(), payment.settlementReference())) {
                issues.add("tax payment has no wallet evidence " + payment.id());
            }
        }
        for (TaxBill bill : taxLedger.bills()) {
            long total = paymentTotals.getOrDefault(bill.id(), 0L);
            if (bill.paidAmount() < 0L || bill.paidAmount() > bill.totalDue()
                    || !TaxPaymentAuditRules.reconciles(total, bill.paidAmount())) {
                issues.add("tax bill payment reconciliation failed " + bill.id());
            }
        }
        TaxCreditSavedData taxCredits = TaxCreditSavedData.get(server);
        Map<String, Long> visibleCreditTotals = new HashMap<>();
        for (var lot : taxCredits.lots()) {
            if (!TaxCreditAuditRules.validLot(lot.taxpayerUuid(), lot.currencyId(), lot.subjectType(),
                    lot.subjectId(), lot.sourceId(), lot.periodStart(), lot.periodEnd(), lot.createdAt(), lot.amount(),
                    Currencies.exists(lot.currencyId()))) {
                issues.add("tax credit lot invalid " + lot.sourceId());
                continue;
            }
            String key = lot.subjectType() + ":" + lot.subjectId() + ":" + lot.taxpayerUuid() + ":" + lot.currencyId();
            visibleCreditTotals.merge(key, lot.amount(), EconomyAuditService::safeAdd);
        }
        for (var balance : taxCredits.balances().entrySet()) {
            if (!TaxCreditAuditRules.aggregateCovers(balance.getValue(), visibleCreditTotals.getOrDefault(balance.getKey(), 0L))) {
                issues.add("tax credit aggregate below visible lots " + balance.getKey());
            }
        }
        for (var invoice : TaxInvoiceSavedData.get(server).invoices()) {
            if ("input".equals(invoice.direction()) && !taxCredits.hasAppliedSource(invoice.sourceEventId())) {
                issues.add("input tax invoice has no applied credit source " + invoice.sourceEventId());
            }
        }
        PopulationSavedData population = PopulationSavedData.get(server);
        for (Household household : population.households()) if (household.cashMinor() < 0L || household.size() < household.workingAge()) issues.add("household " + household.id() + " invalid cash or age structure");
        for (var consumption : HouseholdConsumptionSavedData.get(server).records()) {
            Household household = population.find(consumption.householdId());
            long expected = safeMultiply(consumption.quantity(), consumption.unitPriceMinor());
            if (household == null) issues.add("consumption has no household " + consumption.id());
            if (consumption.id().isBlank() || consumption.itemId().isBlank() || consumption.quantity() <= 0L
                    || consumption.unitPriceMinor() <= 0L || consumption.totalCostMinor() <= 0L
                    || expected != consumption.totalCostMinor()) {
                issues.add("household consumption invalid " + consumption.id());
            }
        }
        HousingLeaseSavedData housing = HousingLeaseSavedData.get(server);
        for (var lease : housing.leases()) {
            Household household = population.find(lease.householdId());
            if (household == null) issues.add("housing lease " + lease.householdId() + " has no household");
            else if (!household.region().equals(lease.region())) {
                issues.add("housing lease " + lease.householdId() + " region differs from household");
            }
            if (lease.dailyRentMinor() < 0L || lease.arrearsMinor() < 0L || lease.depositHeldMinor() < 0L
                    || lease.depositHeldMinor() > lease.depositDueMinor() || lease.missedDays() < 0
                    || lease.missedDays() > 10000) issues.add("housing lease " + lease.householdId() + " invalid balance or status");
        }
        PrivateLandlordSavedData landlords = PrivateLandlordSavedData.get(server);
        landlords.balances().forEach((owner, balance) -> {
            try { java.util.UUID.fromString(owner); } catch (IllegalArgumentException e) { issues.add("private landlord invalid owner " + owner); }
            if (balance == null || balance < 0L) issues.add("private landlord negative receivable " + owner);
        });
        for (var receipt : landlords.receipts()) if (receipt.amount() <= 0L || receipt.balanceAfter() < 0L)
            issues.add("private landlord invalid receipt " + receipt.id());
        for (var withdrawal : landlords.withdrawals()) if (withdrawal.amount() <= 0L || withdrawal.balanceAfter() < 0L)
            issues.add("private landlord invalid withdrawal " + withdrawal.id());
        for (var settlement : LandLeaseSettlementSavedData.get(server).settlements()) {
            if (settlement.id().isBlank() || settlement.landId().isBlank() || settlement.tenantUuid() == null
                    || settlement.ownerUuid() == null || settlement.ownerAmount() < 0L
                    || settlement.tenantRefund() < 0L || settlement.debtAmount() < 0L) {
                issues.add("land lease settlement invalid " + settlement.id());
            }
        }
        for (var debt : LandLeaseDebtSavedData.get(server).debts()) {
            if (debt.id().isBlank() || debt.landId().isBlank() || debt.tenantUuid() == null
                    || debt.ownerUuid() == null || debt.amount() <= 0L) {
                issues.add("land lease debt invalid " + debt.id());
            }
        }
        for (var bill : LandRentBillSavedData.get(server).bills()) {
            if (bill.id().isBlank() || bill.landId().isBlank() || bill.tenantUuid() == null
                    || bill.ownerUuid() == null || bill.amount() <= 0L || bill.dueAt() < 0L
                    || !LandRentBillSavedData.isValidStatus(bill.status())) issues.add("land rent bill invalid " + bill.id());
        }
        Set<String> supplyEscrowIds = new HashSet<>();
        for (var escrow : SupplyEscrowSavedData.get(server).escrows()) {
            long distributed = safeAdd(escrow.heldMinor(), safeAdd(escrow.releasedMinor(), escrow.refundedMinor()));
            if (escrow.originalMinor() <= 0L || escrow.heldMinor() < 0L || escrow.releasedMinor() < 0L
                    || escrow.refundedMinor() < 0L || distributed != escrow.originalMinor()
                    || !supplyEscrowIds.add(escrow.orderId())) {
                issues.add("supply escrow balance mismatch " + escrow.orderId());
            }
            var supplyOrder = com.ailudick.capitalismmod.supply.SupplyMarketSavedData.get(server)
                    .findOrder(escrow.orderId());
            if (escrow.heldMinor() > 0L && supplyOrder == null) {
                issues.add("held supply escrow has no order " + escrow.orderId());
            } else if (supplyOrder != null) {
                if (escrow.quantity() > 0 && (escrow.quantity() != supplyOrder.originalQuantity()
                        || escrow.originalMinor() != safeSupplyAmount(supplyOrder))) {
                    issues.add("supply escrow amount differs from order " + supplyOrder.id());
                }
                String status = com.ailudick.capitalismmod.supply.SupplyOrderAuditService
                        .currentStatus(server, supplyOrder.id());
                if (escrow.heldMinor() > 0L && ("RECEIVED".equals(status) || "LOST".equals(status)
                        || "CANCELLED".equals(status) || "REFUNDED".equals(status))) {
                    issues.add("terminal supply order still holds escrow " + supplyOrder.id());
                }
            }
        }
        BusinessOrderSavedData businessOrders = BusinessOrderSavedData.get(server);
        Set<String> businessEscrowKeys = new HashSet<>();
        for (var escrow : BusinessOrderEscrowSavedData.get(server).escrows()) {
            long distributed = safeAdd(escrow.heldMinor(), safeAdd(escrow.releasedMinor(), escrow.refundedMinor()));
            if (escrow.orderId().isBlank() || escrow.batchId().isBlank() || escrow.buyerId().isBlank()
                    || escrow.originalMinor() <= 0L || escrow.heldMinor() < 0L
                    || escrow.releasedMinor() < 0L || escrow.refundedMinor() < 0L
                    || distributed != escrow.originalMinor()
                    || !businessEscrowKeys.add(escrow.orderId() + ":" + escrow.batchId())) {
                issues.add("business order escrow balance mismatch " + escrow.orderId() + "/" + escrow.batchId());
            }
            var order = businessOrders.get(escrow.orderId());
            if (order == null) {
                issues.add("business order escrow has no order " + escrow.orderId());
            } else if (!BusinessEscrowAuditRules.validBatch(escrow.batchId(), order.quantity(), escrow.quantity())) {
                issues.add("business order escrow batch invalid " + escrow.orderId() + "/" + escrow.batchId());
            } else {
                if (escrow.quantity() > 0) {
                    long paymentMajor = safeMultiply(escrow.quantity(), order.unitPrice());
                    long expectedMinor = paymentMajor < 0L ? Long.MIN_VALUE
                            : ExchangeRates.convert(Money.toMinorSaturated(paymentMajor),
                            com.ailudick.capitalismmod.currency.Currencies.USD, Config.defaultCurrency());
                    if (expectedMinor != escrow.originalMinor()) {
                        issues.add("business order escrow amount differs from quantity "
                                + escrow.orderId() + "/" + escrow.batchId());
                    }
                }
                if (population.find(escrow.buyerId()) == null) {
                    issues.add("business order escrow has no buyer household " + escrow.orderId() + "/" + escrow.batchId());
                }
                if ("completed".equals(order.status()) && escrow.heldMinor() != 0L) {
                    issues.add("completed business order still has held escrow " + escrow.orderId() + "/" + escrow.batchId());
                }
                if (("cancelled".equals(order.status()) || "expired".equals(order.status()))
                        && escrow.heldMinor() != 0L) {
                    issues.add("closed business order still has held escrow " + escrow.orderId() + "/" + escrow.batchId());
                }
            }
        }
        LaborMarketSavedData labor = LaborMarketSavedData.get(server);
        for (EmploymentRecord employment : labor.employments()) if (employment.dailyWageMinor() < 0L || employment.workerId().isBlank() || employment.employerId().isBlank()) issues.add("employment " + employment.id() + " invalid participant or wage");
        for (var entry : labor.offers()) if (entry.vacancies() < 0 || entry.dailyWageMinor() <= 0L) issues.add("job offer " + entry.id() + " invalid vacancy or wage");
        Set<String> activeWorkers = new HashSet<>();
        for (EmploymentRecord employment : labor.employments()) {
            if (!employment.active()) continue;
            if (!activeWorkers.add(employment.workerId())) {
                issues.add("worker has multiple active employments " + employment.workerId());
            }
            if (population.find(employment.workerId()) == null) {
                issues.add("employment worker has no household " + employment.id());
            }
            if (CompanySavedData.get(server).get(employment.employerId()) == null) {
                issues.add("employment employer has no company " + employment.id());
            } else if (!CompanyLifecycleService.canOperate(server, employment.employerId())) {
                issues.add("inactive company has active employment " + employment.id());
            }
        }
        for (var offer : labor.offers()) {
            if (CompanySavedData.get(server).get(offer.employerId()) == null) {
                issues.add("job offer employer has no company " + offer.id());
            }
            if (offer.region().isBlank()) issues.add("job offer has blank region " + offer.id());
        }
        for (var entry : labor.employments()) { var account = LaborPayrollSavedData.get(server).account(entry.id()); if (account.unpaid() < 0L) issues.add("payroll " + entry.id() + " negative arrears"); }
        Set<String> employmentIds = labor.employments().stream().map(EmploymentRecord::id).collect(java.util.stream.Collectors.toSet());
        for (var payroll : LaborPayrollSavedData.get(server).accounts().entrySet()) {
            if (payroll.getKey().isBlank() || payroll.getValue().unpaid() < 0L
                    || payroll.getValue().lastSettlementDay() < -1L) {
                issues.add("payroll account invalid " + payroll.getKey());
            } else if (!employmentIds.contains(payroll.getKey()) && payroll.getValue().unpaid() > 0L) {
                issues.add("payroll claim has no employment " + payroll.getKey());
            }
        }
        EconomicContractSavedData contracts = EconomicContractSavedData.get(server);
        Set<String> contractIds = new HashSet<>();
        for (var contract : contracts.contracts()) {
            if (!contractIds.add(contract.id())
                    || !ContractAuditRules.valid(contract.id(), contract.type(), contract.proposer(),
                    contract.counterparty(), contract.createdAt(), contract.startsAt(), contract.endsAt(),
                    contract.agreedAmountMinor(), contract.currencyId(), contract.status(),
                    contract.fulfilledQuantity(), contract.agreedQuantity(), contract.breachAmountMinor(),
                    Currencies.exists(contract.currencyId()))) {
                issues.add("contract invalid " + contract.id());
            }
        }
        for (var dispute : ContractDisputeSavedData.get(server).disputes()) {
            var contract = contracts.find(dispute.contractId());
            if (contract == null) {
                issues.add("dispute has no contract " + dispute.id());
            } else if ("OPEN".equals(dispute.status()) && contract.status() != ContractStatus.DISPUTED) {
                issues.add("open dispute contract is not disputed " + dispute.id());
            } else if (!"OPEN".equals(dispute.status()) && contract.status() == ContractStatus.DISPUTED) {
                issues.add("resolved dispute contract remains disputed " + dispute.id());
            }
        }
        return List.copyOf(issues);
    }
    public static boolean isBalanceChainValid(List<CompanyLedgerEntry> entries) {
        Map<String, Long> previous = new HashMap<>();
        for (CompanyLedgerEntry entry : entries) {
            if (entry == null || entry.balanceAfter() < 0L) return false;
            Long old = previous.get(entry.currencyId());
            if (old != null && safeAdd(old, entry.amount()) != entry.balanceAfter()) return false;
            previous.put(entry.currencyId(), entry.balanceAfter());
        }
        return true;
    }
    private static long safeAdd(long a, long b) { try { return Math.addExact(a, b); } catch (ArithmeticException e) { return Long.MIN_VALUE; } }
    private static TaxPayment taxLedgerPayment(List<TaxPayment> payments, String paymentId) {
        if (paymentId == null || paymentId.isBlank()) return null;
        return payments.stream().filter(payment -> paymentId.equals(payment.id())).findFirst().orElse(null);
    }
    private static long safeMultiply(long a, long b) { try { return Math.multiplyExact(a, b); } catch (ArithmeticException e) { return Long.MIN_VALUE; } }
    private static boolean isUuid(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            java.util.UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static void auditMailboxMoney(Map<java.util.UUID, Map<String, Long>> balances,
                                          String label, List<String> issues) {
        for (var player : balances.entrySet()) {
            if (player.getKey() == null) issues.add(label + " has no player");
            for (var balance : player.getValue().entrySet()) {
                if (!Currencies.exists(balance.getKey()) || balance.getValue() == null || balance.getValue() <= 0L) {
                    issues.add(label + " invalid " + balance.getKey());
                }
            }
        }
    }

    private static boolean validItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) return false;
        try {
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            return id != null && BuiltInRegistries.ITEM.containsKey(id)
                    && BuiltInRegistries.ITEM.get(id) != net.minecraft.world.item.Items.AIR;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static long safeSupplyAmount(com.ailudick.capitalismmod.supply.PurchaseOrder order) {
        long major = safeMultiply(order.originalQuantity(), order.unitPrice());
        return major < 0L ? Long.MIN_VALUE : com.ailudick.capitalismmod.currency.Money.toMinorSaturated(major);
    }

    private static boolean reconciledOrderQuantity(com.ailudick.capitalismmod.market.MarketOrder order,
                                                   int before, int fill) {
        return order == null ? before == fill : order.quantity() == before || order.quantity() == before - fill;
    }

    private static boolean reconciledOrderQuantity(com.ailudick.capitalismmod.stock.StockOrder order,
                                                   int before, int fill) {
        return order == null ? before == fill : order.quantity() == before || order.quantity() == before - fill;
    }
}
