package com.ailudick.capitalismmod.business;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import com.ailudick.capitalismmod.tax.TaxableIncomeEvent;
import com.ailudick.capitalismmod.tax.IndividualTaxPeriodSavedData;
import com.ailudick.capitalismmod.tax.TaxIncomeVoucherService;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import net.minecraft.server.level.ServerPlayer;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.economy.contract.EconomicContractBridge;
import com.ailudick.capitalismmod.economy.contract.ContractStatus;
import com.ailudick.capitalismmod.population.Household;
import com.ailudick.capitalismmod.population.PopulationSavedData;
import com.ailudick.capitalismmod.market.TradeRegion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/** Registration and account operations for a player's sole proprietorship. */
public final class IndividualBusinessHelper {
    private IndividualBusinessHelper() {
    }

    public static IndividualBusiness get(ServerPlayer player) {
        return IndividualBusinessSavedData.get(player.getServer()).get(player.getUUID());
    }

    public static void recordTaxableIncome(ServerPlayer player, IndividualBusiness business, String sourceId,
                                           long income, long occurredAt) {
        if (business == null || income <= 0L) return;
        long period = PerpetualCalendar.ticksForDays(90L);
        long end = ((occurredAt / period) + 1L) * period;
        IndividualTaxPeriodSavedData periods = IndividualTaxPeriodSavedData.get(player.getServer());
        if (periods.isClosed(business.businessId(), end)) return;
        periods.recordIncome(sourceId, business.businessId(),
                business.ownerUuid(), Currencies.USD.id(), income, end - period, end);
    }

    public static void recordTaxableExpense(ServerPlayer player, IndividualBusiness business, String sourceId,
                                            long expense, long occurredAt) {
        recordTaxableExpense(player, business, sourceId, expense, occurredAt, "");
    }

    public static void recordTaxableExpense(ServerPlayer player, IndividualBusiness business, String sourceId,
                                            long expense, long occurredAt, String details) {
        if (business == null || expense <= 0L) return;
        long period = PerpetualCalendar.ticksForDays(90L);
        long end = ((occurredAt / period) + 1L) * period;
        IndividualTaxPeriodSavedData periods = IndividualTaxPeriodSavedData.get(player.getServer());
        if (periods.isClosed(business.businessId(), end)) return;
        periods.recordExpense(sourceId, business.businessId(),
                business.ownerUuid(), Currencies.USD.id(), expense, end - period, end);
        com.ailudick.capitalismmod.tax.TaxExpenseService.record(player.getServer(), business.ownerUuid(),
                business.businessId(), "individual_business_expense", Currencies.USD.id(), expense,
                occurredAt, sourceId, true, details);
    }

    public static boolean register(ServerPlayer player, String name, String scope) {
        if (get(player) != null || name == null || scope == null || !BusinessTypes.isValid(scope)) {
            return false;
        }
        String trimmedName = name.trim();
        String trimmedScope = scope.trim();
        if (trimmedName.isEmpty() || trimmedName.length() > 32 || trimmedScope.isEmpty() || trimmedScope.length() > 32) {
            return false;
        }
        IndividualBusinessSavedData.get(player.getServer()).put(
                IndividualBusiness.create(player.getUUID(), trimmedName, trimmedScope));
        IndividualBusiness business = get(player);
        BusinessLedgerSavedData.get(player.getServer()).append(new BusinessLedgerEntry(
                business.businessId(), player.level().getGameTime(), "registration", "", 0L, 0L,
                "个体户登记"));
        return true;
    }

    public static boolean deposit(ServerPlayer player, long amount) {
        IndividualBusiness business = get(player);
        long amountMinor = Money.toMinor(amount);
        if (amount <= 0 || amountMinor <= 0 || business == null || !business.status().equals("active")
                || amount > Long.MAX_VALUE - business.balance("usd")) {
            return false;
        }
        if (!EconomyHelper.consumeItemsWithChange(player, Currencies.USD, amountMinor)) {
            return false;
        }
        Map<String, Long> account = new HashMap<>(business.account());
        account.put("usd", business.balance("usd") + amount);
        IndividualBusinessSavedData.get(player.getServer()).put(business.withAccount(account));
        BusinessLedgerSavedData.get(player.getServer()).append(new BusinessLedgerEntry(
                business.businessId(), player.level().getGameTime(), "deposit", "usd", amount,
                business.balance("usd") + amount, "业主存入经营资金"));
        return true;
    }

    public static boolean withdraw(ServerPlayer player, long amount) {
        IndividualBusiness business = get(player);
        if (business == null || !business.status().equals("active") || amount <= 0 || business.balance("usd") < amount) {
            return false;
        }
        String id = java.util.UUID.randomUUID().toString();
        IndividualWithdrawalIntentSavedData intents = IndividualWithdrawalIntentSavedData.get(player.getServer());
        intents.add(new IndividualWithdrawalIntentSavedData.Intent(id, player.getUUID(), business.businessId(), "usd",
                business.balance("usd"), amount, false));
        settleWithdrawal(player, intents, intents.find(id));
        return intents.find(id) == null;
    }

    /** Replays withdrawals interrupted between the business debit and cash delivery. */
    public static int recoverWithdrawals(ServerPlayer player) {
        if (player == null || player.getServer() == null) return 0;
        IndividualWithdrawalIntentSavedData data = IndividualWithdrawalIntentSavedData.get(player.getServer());
        int recovered = 0;
        for (IndividualWithdrawalIntentSavedData.Intent intent : data.intents()) {
            if (!player.getUUID().equals(intent.player())) continue;
            if (settleWithdrawal(player, data, intent)) recovered++;
        }
        return recovered;
    }

    private static boolean settleWithdrawal(ServerPlayer player, IndividualWithdrawalIntentSavedData data,
                                            IndividualWithdrawalIntentSavedData.Intent intent) {
        if (intent == null || !"usd".equals(intent.currencyId())) return false;
        IndividualBusiness business = IndividualBusinessSavedData.get(player.getServer()).findByBusinessId(intent.businessId());
        if (business == null || !player.getUUID().equals(business.ownerUuid())) return false;
        long after = intent.balanceBefore() - intent.amount();
        if (business.balance(intent.currencyId()) == intent.balanceBefore()) {
            Map<String, Long> account = new HashMap<>(business.account());
            account.put(intent.currencyId(), after);
            IndividualBusinessSavedData.get(player.getServer()).put(business.withAccount(account));
            BusinessLedgerSavedData.get(player.getServer()).append(new BusinessLedgerEntry(
                    business.businessId(), player.level().getGameTime(), "owner_withdrawal", intent.currencyId(),
                    -intent.amount(), after, "业主提款 [source=" + intent.id() + "]"));
            data.markAccountApplied(intent.id());
        }
        IndividualBusiness current = IndividualBusinessSavedData.get(player.getServer()).findByBusinessId(intent.businessId());
        if (current == null || current.balance(intent.currencyId()) != after) return false;
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(player.getServer());
        String source = "individual-withdrawal:" + intent.id();
        if (!mailbox.hasTransferSource(source)) mailbox.creditTransferOnce(intent.player(), intent.currencyId(), Money.toMinor(intent.amount()), source);
        mailbox.redeemTransferOnly(player);
        data.remove(intent.id());
        return true;
    }

    public static boolean close(ServerPlayer player) {
        IndividualBusiness business = get(player);
        if (business == null || business.balance("usd") != 0 || business.taxOwed() != 0) {
            return false;
        }
        IndividualBusinessSavedData.get(player.getServer()).put(business.withStatus("closed"));
        BusinessLedgerSavedData.get(player.getServer()).append(new BusinessLedgerEntry(
                business.businessId(), player.level().getGameTime(), "closure", "", 0L,
                business.balance("usd"), "个体户注销"));
        return true;
    }

    /** Pays all accrued sole-proprietor income tax through the unified tax ledger. */
    public static boolean payTax(ServerPlayer player) {
        IndividualBusiness business = get(player);
        if (business == null) return false;
        TaxSubject subject = new TaxSubject(TaxType.INDIVIDUAL_BUSINESS_INCOME,
                business.businessId(), business.ownerUuid());
        long outstanding = TaxService.outstanding(player.getServer(), subject);
        if (outstanding <= 0L && business.taxOwed() > 0L) {
            long legacyAmount = Money.toMinor(business.taxOwed());
            if (legacyAmount <= 0L) return false;
            TaxService.ensureOutstanding(player.getServer(), subject, "usd", legacyAmount,
                    player.level().getGameTime(), 0L, 0L);
        }
        outstanding = TaxService.outstanding(player.getServer(), subject);
        if (outstanding <= 0L || !TaxService.pay(player, subject, outstanding)) return false;
        IndividualBusinessSavedData.get(player.getServer()).put(business.afterSettlement(business.account(), 0L));
        return true;
    }

    /** Updates the legacy sole-proprietor tax mirror after a unified tax payment. */
    public static void syncTaxMirror(ServerPlayer player, long outstandingMinor) {
        IndividualBusiness business = get(player);
        if (business != null) {
            IndividualBusinessSavedData.get(player.getServer()).put(
                    business.afterSettlement(business.account(), Money.toMajorCeiling(outstandingMinor)));
        }
    }

    public static BusinessOrder createOrder(ServerPlayer player, String itemId, int quantity, long unitPrice, int days) {
        IndividualBusiness business = get(player);
        Item item = parseItem(itemId);
        if (business == null || !"active".equals(business.status()) || item == null || quantity <= 0
                || unitPrice <= 0 || days <= 0 || days > 365) {
            return null;
        }
        String id = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        long now = player.level().getGameTime();
        BusinessOrder order = new BusinessOrder(id, business.businessId(), player.getUUID(), itemId, quantity,
                quantity, unitPrice, now, now + PerpetualCalendar.ticksForDays(days), "open");
        BusinessOrderSavedData.get(player.getServer()).put(order);
        EconomicContractBridge.businessOrderCreated(player.getServer(), order);
        BusinessLedgerSavedData.get(player.getServer()).append(new BusinessLedgerEntry(
                business.businessId(), now, "order_created", "", 0L, business.balance("usd"),
                "发布销售订单 " + id + "：" + itemId + " x" + quantity));
        return order;
    }

    public static boolean deliverOrder(ServerPlayer player, String orderId) {
        BusinessOrderSavedData orderData = BusinessOrderSavedData.get(player.getServer());
        BusinessOrder order = orderData.get(orderId);
        IndividualBusiness business = get(player);
        if (order == null || business == null || !order.sellerUuid().equals(player.getUUID())
                || !order.businessId().equals(business.businessId()) || !"open".equals(order.status())) {
            return false;
        }
        long now = player.level().getGameTime();
        String goodsSource = order.businessId() + ":order:" + order.id() + ":goods";
        if (now > order.deadline() && !WarehouseSavedData.get(player.getServer()).hasConsumedSource(goodsSource)) {
            long payment = Math.multiplyExact((long) order.remaining(), order.unitPrice());
            if (!refundBuyer(player, order, payment)) return false;
            orderData.put(order.withStatus("expired"));
            EconomicContractBridge.businessOrderEvent(player.getServer(), order, ContractStatus.EXPIRED, now);
            return false;
        }
        Item item = parseItem(order.itemId());
        WarehouseSavedData warehouse = WarehouseSavedData.get(player.getServer());
        boolean goodsConsumed = warehouse.hasConsumedSource(goodsSource);
        if (item == null || (!goodsConsumed && warehouse.count(player.getUUID(), order.itemId()) < order.remaining())) {
            return false;
        }
        long payment = Math.multiplyExact((long) order.remaining(), order.unitPrice());
        String source = business.businessId() + ":order:" + order.id();
        String buyerSource = source + ":buyer";
        PopulationSavedData population = PopulationSavedData.get(player.getServer());
        String buyerId = population.chargedHousehold(buyerSource);
        if (buyerId == null) {
            String region = TradeRegion.of(player.blockPosition());
            long paymentMinor = ExchangeRates.convert(Money.toMinorSaturated(payment), Currencies.USD, Config.defaultCurrency());
            buyerId = population.households().stream()
                    .filter(h -> h.id().startsWith("npc-") && region.equals(h.region()) && h.cashMinor() >= paymentMinor)
                    .map(Household::id).findFirst().orElse(null);
            if (buyerId == null || !population.chargeCashOnce(buyerId, paymentMinor, buyerSource)) return false;
        }
        if (!goodsConsumed && !warehouse.consumeOnce(InventoryOwner.player(player.getUUID()), item, order.remaining(), goodsSource)) return false;
        BusinessLedgerSavedData ledger = BusinessLedgerSavedData.get(player.getServer());
        BusinessLedgerEntry settlement = ledger.findSource(business.businessId(), source);
        if (settlement == null) {
            long newBalance = Math.addExact(business.balance("usd"), payment);
            ledger.append(new BusinessLedgerEntry(business.businessId(), now, "order_payment", "usd", payment,
                    newBalance, "完成销售订单 " + order.id() + "，交付 " + order.itemId() + " x" + order.quantity()
                            + " [source=" + source + "]"));
            Map<String, Long> account = new HashMap<>(business.account());
            account.put("usd", newBalance);
            IndividualBusinessSavedData.get(player.getServer()).put(business.withAccount(account));
        } else if (business.balance("usd") != settlement.balanceAfter()) {
            Map<String, Long> account = new HashMap<>(business.account());
            account.put("usd", settlement.balanceAfter());
            IndividualBusinessSavedData.get(player.getServer()).put(business.withAccount(account));
        }
        orderData.put(order.withDelivery(0, "completed"));
        EconomicContractBridge.businessOrderEvent(player.getServer(), order, ContractStatus.COMPLETED, now);
        recordTaxableIncome(player, business, business.businessId() + ":order:" + order.id(), payment, now);
        TaxIncomeVoucherService.record(player.getServer(), business.ownerUuid(), business.businessId(),
                "individual_business_income", Currencies.USD.id(), payment, now,
                business.businessId() + ":income:" + order.id(),
                order.itemId() + " x" + order.quantity() + " from order " + order.id());
        return true;
    }

    /** Completes only orders whose inventory escrow was already consumed. */
    public static int recoverOrders(ServerPlayer player) {
        if (player == null || player.getServer() == null) return 0;
        BusinessOrderSavedData orders = BusinessOrderSavedData.get(player.getServer());
        WarehouseSavedData warehouse = WarehouseSavedData.get(player.getServer());
        int recovered = 0;
        for (BusinessOrder order : orders.orders().values()) {
            if (!player.getUUID().equals(order.sellerUuid()) || !"open".equals(order.status())) continue;
            String source = order.businessId() + ":order:" + order.id() + ":goods";
            if (warehouse.hasConsumedSource(source) && deliverOrder(player, order.id())) recovered++;
        }
        return recovered;
    }

    public static boolean cancelOrder(ServerPlayer player, String orderId) {
        BusinessOrder order = BusinessOrderSavedData.get(player.getServer()).get(orderId);
        IndividualBusiness business = get(player);
        if (order == null || business == null || !order.sellerUuid().equals(player.getUUID())
                || !order.businessId().equals(business.businessId()) || !"open".equals(order.status())) {
            return false;
        }
        String goodsSource = order.businessId() + ":order:" + order.id() + ":goods";
        if (WarehouseSavedData.get(player.getServer()).hasConsumedSource(goodsSource)) return false;
        long payment = Math.multiplyExact((long) order.remaining(), order.unitPrice());
        if (!refundBuyer(player, order, payment)) return false;
        BusinessOrderSavedData.get(player.getServer()).put(order.withStatus("cancelled"));
        EconomicContractBridge.businessOrderEvent(player.getServer(), order, ContractStatus.CANCELLED,
                player.level().getGameTime());
        BusinessLedgerSavedData.get(player.getServer()).append(new BusinessLedgerEntry(
                business.businessId(), player.level().getGameTime(), "order_cancelled", "", 0L,
                business.balance("usd"), "取消销售订单 " + order.id()));
        return true;
    }

    /** Returns a previously charged NPC buyer's funds exactly once. */
    private static boolean refundBuyer(ServerPlayer player, BusinessOrder order, long paymentMajor) {
        String source = order.businessId() + ":order:" + order.id();
        PopulationSavedData population = PopulationSavedData.get(player.getServer());
        String buyerId = population.chargedHousehold(source + ":buyer");
        if (buyerId == null) return true;
        long paymentMinor = ExchangeRates.convert(Money.toMinorSaturated(paymentMajor), Currencies.USD, Config.defaultCurrency());
        String refundSource = source + ":buyer:refund";
        return population.hasCreditedSource(refundSource)
                || population.addCashOnce(buyerId, paymentMinor, refundSource);
    }

    private static Item parseItem(String itemId) {
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            return item == null || item == Items.AIR ? null : item;
        } catch (IllegalArgumentException | NullPointerException exception) {
            return null;
        }
    }

}
