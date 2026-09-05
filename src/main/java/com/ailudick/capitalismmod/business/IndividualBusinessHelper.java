package com.ailudick.capitalismmod.business;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import com.ailudick.capitalismmod.tax.TaxableIncomeEvent;
import com.ailudick.capitalismmod.tax.IndividualTaxPeriodSavedData;
import com.ailudick.capitalismmod.tax.TaxIncomeVoucherService;
import net.minecraft.server.level.ServerPlayer;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
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
        long period = 90L * 24000L;
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
        long period = 90L * 24000L;
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
        Map<String, Long> account = new HashMap<>(business.account());
        account.put("usd", business.balance("usd") - amount);
        IndividualBusinessSavedData.get(player.getServer()).put(business.withAccount(account));
        EconomyHelper.giveMoney(player, Currencies.USD, Money.toMinor(amount));
        BusinessLedgerSavedData.get(player.getServer()).append(new BusinessLedgerEntry(
                business.businessId(), player.level().getGameTime(), "owner_withdrawal", "usd", -amount,
                business.balance("usd") - amount, "业主提款"));
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
                    business.afterSettlement(business.account(), Math.max(0L, outstandingMinor / 100L)));
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
                quantity, unitPrice, now, now + days * 24000L, "open");
        BusinessOrderSavedData.get(player.getServer()).put(order);
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
        if (now > order.deadline()) {
            orderData.put(order.withStatus("expired"));
            return false;
        }
        Item item = parseItem(order.itemId());
        WarehouseSavedData warehouse = WarehouseSavedData.get(player.getServer());
        if (item == null || warehouse.count(player.getUUID(), order.itemId()) < order.remaining()) {
            return false;
        }
        warehouse.consume(player.getUUID(), item, order.remaining());
        long payment = Math.multiplyExact((long) order.remaining(), order.unitPrice());
        Map<String, Long> account = new HashMap<>(business.account());
        long newBalance = Math.addExact(business.balance("usd"), payment);
        account.put("usd", newBalance);
        IndividualBusinessSavedData.get(player.getServer()).put(business.withAccount(account));
        orderData.put(order.withDelivery(0, "completed"));
        recordTaxableIncome(player, business, business.businessId() + ":order:" + order.id(), payment, now);
        TaxIncomeVoucherService.record(player.getServer(), business.ownerUuid(), business.businessId(),
                "individual_business_income", Currencies.USD.id(), payment, now,
                business.businessId() + ":income:" + order.id(),
                order.itemId() + " x" + order.quantity() + " from order " + order.id());
        BusinessLedgerSavedData.get(player.getServer()).append(new BusinessLedgerEntry(
                business.businessId(), now, "order_payment", "usd", payment, newBalance,
                "完成销售订单 " + order.id() + "，交付 " + order.itemId() + " x" + order.quantity()));
        return true;
    }

    public static boolean cancelOrder(ServerPlayer player, String orderId) {
        BusinessOrder order = BusinessOrderSavedData.get(player.getServer()).get(orderId);
        IndividualBusiness business = get(player);
        if (order == null || business == null || !order.sellerUuid().equals(player.getUUID())
                || !order.businessId().equals(business.businessId()) || !"open".equals(order.status())) {
            return false;
        }
        BusinessOrderSavedData.get(player.getServer()).put(order.withStatus("cancelled"));
        BusinessLedgerSavedData.get(player.getServer()).append(new BusinessLedgerEntry(
                business.businessId(), player.level().getGameTime(), "order_cancelled", "", 0L,
                business.balance("usd"), "取消销售订单 " + order.id()));
        return true;
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
