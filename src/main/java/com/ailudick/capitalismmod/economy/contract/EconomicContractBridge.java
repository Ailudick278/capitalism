package com.ailudick.capitalismmod.economy.contract;

import com.ailudick.capitalismmod.company.CompanyFreightContractSavedData;
import com.ailudick.capitalismmod.economy.expansion.EconomicActorRef;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.business.BusinessOrder;
import com.ailudick.capitalismmod.supply.SupplyOrderAuditSavedData;
import com.ailudick.capitalismmod.economy.labor.EmploymentRecord;
import com.ailudick.capitalismmod.util.EconomyMath;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;
import java.util.LinkedHashMap;

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
        if (status == ContractStatus.ACTIVE || status == ContractStatus.COMPLETED) {
            if (current.status() == ContractStatus.OFFERED) data.transition(order.id(), ContractStatus.ACTIVE, at);
            current = data.find(order.id());
            long delivered = Math.max(0L, (long) order.quantity() - order.remaining());
            if (current != null && delivered > current.fulfilledQuantity()) {
                data.fulfill(order.id(), delivered - current.fulfilledQuantity());
            }
        }
        data.transition(order.id(), status, at);
    }

    public static void employmentCreated(MinecraftServer server, EmploymentRecord employment) {
        if (server == null || employment == null) return;
        EconomicContractSavedData data = EconomicContractSavedData.get(server);
        if (data.find(employment.id()) != null) return;
        long endsAt = employment.endedAt() > 0L ? employment.endedAt() : Long.MAX_VALUE;
        data.add(new EconomicContract(employment.id(), ContractType.EMPLOYMENT,
                new EconomicActorRef("company", employment.employerId()),
                new EconomicActorRef(employment.workerId().startsWith("npc-") ? "npc" : "player", employment.workerId()),
                employment.startedAt(), employment.startedAt(), endsAt, employment.dailyWageMinor(),
                Currencies.USD.id(), ContractStatus.ACTIVE, 0L, 0L, 0L));
    }

    public static void employmentEnded(MinecraftServer server, String employmentId, long at) {
        status(server, employmentId, ContractStatus.COMPLETED, at);
    }

    public static void employmentBreach(MinecraftServer server, String employmentId, long arrearsMinor, long at) {
        if (server == null || employmentId == null || arrearsMinor <= 0L) return;
        EconomicContractSavedData data = EconomicContractSavedData.get(server);
        EconomicContract current = data.find(employmentId);
        if (current != null && (current.status() == ContractStatus.ACTIVE || current.status() == ContractStatus.OFFERED)) {
            data.breach(employmentId, arrearsMinor);
        }
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
        long now = server.overworld().getGameTime();
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
                    generic.transition(current.id(), ContractStatus.ACTIVE, now);
                }
                generic.transition(current.id(), next, now);
            }
        }
    }

    /** Rebuilds supply-contract status from the durable domain event stream. */
    public static void syncSupply(MinecraftServer server) {
        if (server == null) return;
        EconomicContractSavedData generic = EconomicContractSavedData.get(server);
        LinkedHashMap<String, SupplyOrderAuditSavedData.Event> created = new LinkedHashMap<>();
        for (SupplyOrderAuditSavedData.Event event : SupplyOrderAuditSavedData.get(server).events()) {
            if ("CREATED".equals(event.type())) created.putIfAbsent(event.orderId(), event);
        }
        for (var entry : created.entrySet()) {
            String orderId = entry.getKey();
            SupplyOrderAuditSavedData.Event first = entry.getValue();
            if (generic.find(orderId) == null) {
                supplyCreated(server, orderId, first.buyerUuid(), first.supplierUuid(), first.itemId(),
                        first.quantity(), first.amount(), first.occurredAt());
            }
            EconomicContract current = generic.find(orderId);
            if (current == null) continue;
            var events = SupplyOrderAuditSavedData.get(server).forOrder(orderId);
            String status = com.ailudick.capitalismmod.supply.SupplyOrderAuditService.currentStatus(server, orderId);
            if ("RECEIVED".equals(status)) {
                if (current.status() == ContractStatus.OFFERED) generic.transition(orderId, ContractStatus.ACTIVE, first.occurredAt());
                current = generic.find(orderId);
                long delivered = events.stream().filter(e -> "DELIVERED".equals(e.type()))
                        .mapToLong(SupplyOrderAuditSavedData.Event::quantity).sum();
                long delta = Math.max(0L, delivered - (current == null ? 0L : current.fulfilledQuantity()));
                if (delta > 0L) generic.fulfill(orderId, delta);
                current = generic.find(orderId);
                if (current != null && current.status() == ContractStatus.ACTIVE
                        && current.agreedQuantity() > 0L && current.fulfilledQuantity() >= current.agreedQuantity()) {
                    generic.transition(orderId, ContractStatus.COMPLETED, server.overworld().getGameTime());
                }
            } else if ("LOST".equals(status)) {
                if (current.status() == ContractStatus.OFFERED || current.status() == ContractStatus.ACTIVE) generic.transition(orderId, ContractStatus.BREACHED, server.overworld().getGameTime());
            } else if ("CANCELLED".equals(status)) {
                if (current.status() == ContractStatus.OFFERED || current.status() == ContractStatus.ACTIVE) generic.transition(orderId, ContractStatus.CANCELLED, server.overworld().getGameTime());
            } else if ("REFUNDED".equals(status)) {
                if (current.status() == ContractStatus.OFFERED || current.status() == ContractStatus.ACTIVE) generic.transition(orderId, ContractStatus.EXPIRED, server.overworld().getGameTime());
            } else if (!"PLACED".equals(status) && current.status() == ContractStatus.OFFERED) {
                generic.transition(orderId, ContractStatus.ACTIVE, server.overworld().getGameTime());
            }
        }
    }
}
