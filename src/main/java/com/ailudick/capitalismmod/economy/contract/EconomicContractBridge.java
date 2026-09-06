package com.ailudick.capitalismmod.economy.contract;

import com.ailudick.capitalismmod.company.CompanyFreightContractSavedData;
import com.ailudick.capitalismmod.economy.expansion.EconomicActorRef;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.business.BusinessOrder;
import com.ailudick.capitalismmod.util.EconomyMath;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/** Mirrors the existing freight domain lifecycle into the generic contract index. */
public final class EconomicContractBridge {
    private EconomicContractBridge() {}
    public static void offered(MinecraftServer server, CompanyFreightContractSavedData.Contract c) {
        if (server == null || c == null) return;
        EconomicContractSavedData data = EconomicContractSavedData.get(server);
        if (data.find(c.id()) == null) data.add(new EconomicContract(c.id(), ContractType.FREIGHT,
                new EconomicActorRef("company", c.buyerCompanyId()), new EconomicActorRef("company", c.carrierCompanyId()),
                c.createdAt(), c.createdAt(), c.expiresAt(), c.quotedCost(), "usd", ContractStatus.OFFERED, 0L, 0L));
    }
    public static void status(MinecraftServer server, String id, ContractStatus status, long at) {
        if (server == null || id == null || status == null) return;
        EconomicContractSavedData.get(server).transition(id, status, at);
    }

    public static void businessOrderCreated(MinecraftServer server, BusinessOrder order) {
        if (server == null || order == null) return;
        EconomicContractSavedData data = EconomicContractSavedData.get(server);
        if (data.find(order.id()) != null) return;
        long amount = EconomyMath.multiply(Money.toMinor(order.unitPrice()), order.quantity());
        data.add(new EconomicContract(order.id(), ContractType.TRADE,
                new EconomicActorRef("individual_business", order.businessId()),
                new EconomicActorRef("npc_market", "system"), order.createdTick(), order.createdTick(), order.deadline(),
                Math.max(0L, amount), Currencies.USD.id(), ContractStatus.OFFERED, 0L, order.quantity(), 0L));
    }

    public static void businessOrderEvent(MinecraftServer server, BusinessOrder order, ContractStatus status, long at) {
        if (server == null || order == null) return;
        EconomicContractSavedData data = EconomicContractSavedData.get(server);
        EconomicContract current = data.find(order.id());
        if (current == null) { businessOrderCreated(server, order); current = data.find(order.id()); }
        if (current == null) return;
        if (status == ContractStatus.COMPLETED) {
            if (current.status() == ContractStatus.OFFERED) data.transition(order.id(), ContractStatus.ACTIVE, at);
            data.fulfill(order.id(), order.quantity());
        }
        data.transition(order.id(), status, at);
    }

    public static void supplyCreated(MinecraftServer server, String id, UUID buyer, UUID supplier,
                                     String itemId, int quantity, long amountMajor, long createdAt) {
        if (server == null || id == null || id.isBlank() || buyer == null || supplier == null
                || itemId == null || itemId.isBlank() || quantity <= 0 || amountMajor <= 0L) return;
        EconomicContractSavedData data = EconomicContractSavedData.get(server);
        if (data.find(id) != null) return;
        long lifetime = PerpetualCalendar.ticksForDays(Config.SUPPLY_ORDER_EXPIRY_DAYS.get());
        long endsAt = lifetime > 0L && createdAt <= Long.MAX_VALUE - lifetime ? createdAt + lifetime : createdAt;
        data.add(new EconomicContract(id, ContractType.SUPPLY,
                new EconomicActorRef("player", buyer.toString()), new EconomicActorRef("player", supplier.toString()),
                Math.max(0L, createdAt), Math.max(0L, createdAt), endsAt,
                EconomyMath.multiply(amountMajor, 100L), Currencies.USD.id(), ContractStatus.OFFERED, 0L, quantity, 0L));
    }

    public static void supplyEvent(MinecraftServer server, String id, String eventType, int quantity, long at) {
        if (server == null || id == null || id.isBlank() || eventType == null) return;
        EconomicContractSavedData data = EconomicContractSavedData.get(server);
        EconomicContract current = data.find(id);
        if (current == null) return;
        if ("DELIVERED".equals(eventType) && current.status() == ContractStatus.OFFERED) {
            data.transition(id, ContractStatus.ACTIVE, at); current = data.find(id);
        }
        if (current == null) return;
        if ("DELIVERED".equals(eventType) && current.status() == ContractStatus.ACTIVE) {
            data.fulfill(id, quantity);
            EconomicContract progressed = data.find(id);
            if (progressed != null && progressed.agreedQuantity() > 0L
                    && progressed.fulfilledQuantity() >= progressed.agreedQuantity()) {
                data.transition(id, ContractStatus.COMPLETED, at);
            }
        }
        ContractStatus next = switch (eventType) {
            case "BACKORDERED", "PARTIAL", "DISPATCHED" -> ContractStatus.ACTIVE;
            case "FULFILLED" -> null;
            case "CANCELLED_REFUND" -> ContractStatus.CANCELLED;
            case "EXPIRED_REFUND", "EXPIRED_REFUND_COMPANY",
                 "INTENT_EXPIRED_REFUND", "INTENT_EXPIRED_REFUND_COMPANY" -> ContractStatus.EXPIRED;
            case "PARTIAL_LOSS" -> ContractStatus.ACTIVE;
            case "LOST" -> ContractStatus.BREACHED;
            default -> null;
        };
        if (next != null && data.find(id) != null && data.find(id).status() != next) {
            if (data.find(id).status() == ContractStatus.OFFERED && next == ContractStatus.COMPLETED) data.transition(id, ContractStatus.ACTIVE, at);
            data.transition(id, next, at);
        }
    }

    public static void syncFreight(MinecraftServer server) {
        if (server == null) return;
        EconomicContractSavedData generic = EconomicContractSavedData.get(server);
        for (CompanyFreightContractSavedData.Contract freight : CompanyFreightContractSavedData.get(server).contracts()) {
            EconomicContract current = generic.find(freight.id());
            if (current == null) { offered(server, freight); current = generic.find(freight.id()); }
            if (current == null) continue;
            ContractStatus next = switch (freight.status()) {
                case "accepted" -> ContractStatus.ACTIVE;
                case "settled" -> ContractStatus.COMPLETED;
                case "cancelled" -> ContractStatus.CANCELLED;
                case "expired" -> ContractStatus.EXPIRED;
                case "loss" -> ContractStatus.BREACHED;
                default -> ContractStatus.OFFERED;
            };
            if (current.status() != next) {
                if (current.status() == ContractStatus.OFFERED && next == ContractStatus.COMPLETED) {
                    generic.transition(current.id(), ContractStatus.ACTIVE, freight.expiresAt());
                }
                generic.transition(current.id(), next, freight.expiresAt());
            }
        }
    }
}
