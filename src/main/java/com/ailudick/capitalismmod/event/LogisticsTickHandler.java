package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.market.LogisticsSavedData;
import com.ailudick.capitalismmod.market.LogisticsLossService;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.market.LogisticsInfrastructureSavedData;
import com.ailudick.capitalismmod.market.LogisticsClaimSavedData;
import com.ailudick.capitalismmod.market.LogisticsDeliverySavedData;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.supply.SupplyOrderAuditService;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanyInventoryCostSavedData;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.company.CompanyLogisticsCostSavedData;
import com.ailudick.capitalismmod.market.LogisticsCostSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;

/** Delivers due cargo into persistent warehouses, including for offline buyers. */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class LogisticsTickHandler {
    private static final int CHECK_INTERVAL = 20;
    private static int tickCounter;

    private LogisticsTickHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;
        MinecraftServer server = event.getServer();
        LogisticsSavedData data = LogisticsSavedData.get(server);
        LogisticsDeliverySavedData deliveries = LogisticsDeliverySavedData.get(server);
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        long now = server.overworld().getGameTime();
        for (LogisticsSavedData.Shipment shipment : new ArrayList<>(data.shipments())) {
            if (shipment.deliveryTick() > now) {
                continue;
            }
            if (deliveries.hasShipment(shipment.id())) {
                // A previous delivery was committed but the shipment removal was
                // interrupted. Do not route the residual shipment through risk or
                // credit the warehouse a second time.
                data.remove(shipment.id());
                continue;
            }
            double risk = Config.LOGISTICS_RISK_RATE.get()
                    * (1.0 - LogisticsInfrastructureSavedData.get(server).riskReduction(
                    shipment.originRegion(), shipment.destinationRegion(), shipment.transport()));
            if (risk > 0.0 && Math.random() < risk) {
                if (shipment.insured()) {
                    LogisticsClaimSavedData claims = LogisticsClaimSavedData.get(server);
                    // If a previous tick completed the payout but failed before removing
                    // the shipment, never pay the same shipment a second time.
                    if (claims.hasShipment(shipment.id())) {
                        data.remove(shipment.id());
                        continue;
                    }
                    long insuredValue;
                    try {
                        insuredValue = Math.multiplyExact((long) shipment.quantity(), Config.LOGISTICS_DECLARED_VALUE.get());
                    } catch (ArithmeticException e) {
                        insuredValue = Long.MAX_VALUE;
                    }
                    long actualLoss = actualLoss(shipment, insuredValue);
                    long payout = Math.min(insuredValue, actualLoss);
                    Company company = shipment.buyerCompanyId().isBlank()
                            ? null : CompanySavedData.get(server).get(shipment.buyerCompanyId());
                    boolean companyShipment = company != null && company.ownerUuid().equals(shipment.buyer());
                    if (companyShipment) {
                        CompanyInventoryCostSavedData.get(server).consume(company.companyId(),
                                shipment.itemId(), shipment.quantity());
                        CompanyHelper.creditTreasuryNonOperating(server, company.companyId(), "usd", payout,
                                "cargo_insurance_claim", "Cargo insurance indemnity");
                    } else {
                        MarketMailboxSavedData.get(server).creditMoney(shipment.buyer(), "usd", Money.toMinor(payout));
                    }
                    claims.settle(new LogisticsClaimSavedData.Claim(
                            java.util.UUID.randomUUID().toString(), shipment.id(), shipment.buyer(), insuredValue,
                            actualLoss, payout, now, "settled"));
                    data.remove(shipment.id());
                } else if (shipment.disruptionCount() + 1 >= Config.LOGISTICS_MAX_DISRUPTIONS.get()) {
                    LogisticsLossService.record(server, shipment);
                    data.remove(shipment.id());
                } else {
                    data.replace(new LogisticsSavedData.Shipment(shipment.id(), shipment.buyer(), shipment.itemId(),
                            shipment.quantity(), now + Config.LOGISTICS_DISRUPTION_TICKS.get(), shipment.originRegion(),
                            shipment.destinationRegion(), shipment.transport(), false,
                            shipment.disruptionCount() + 1, shipment.supplyOrderId(), shipment.buyerCompanyId(),
                            shipment.unitPrice(), shipment.supplierUuid()));
                }
                continue;
            }
            Item item = parseItem(shipment.itemId());
            if (item != null) {
                InventoryOwner owner = shipment.buyerCompanyId().isBlank()
                        ? InventoryOwner.player(shipment.buyer())
                        : InventoryOwner.company(shipment.buyerCompanyId());
                warehouse.credit(owner, item, shipment.quantity());
                if (!shipment.buyerCompanyId().isBlank()) {
                    Company company = CompanySavedData.get(server).get(shipment.buyerCompanyId());
                    LogisticsCostSavedData.FuelPlan fuelPlan = LogisticsCostSavedData.get(server)
                            .find(shipment.id());
                    if (company != null && fuelPlan != null
                            && shipment.buyerCompanyId().equals(fuelPlan.buyerCompanyId())
                            && fuelPlan.estimatedCost() >= 0L) {
                        CompanyInventoryCostSavedData.get(server).addFreightCost(
                                company.companyId(), shipment.itemId(), fuelPlan.estimatedCost(), shipment.id());
                        CompanyLogisticsCostSavedData.get(server).record(
                                new CompanyLogisticsCostSavedData.CapitalizedCost(
                                        shipment.id(), company.companyId(), shipment.itemId(),
                                        shipment.quantity(), fuelPlan.estimatedCost(), now));
                    }
                }
                if (!shipment.supplyOrderId().isBlank() && shipment.supplierUuid() != null) {
                    SupplyOrderAuditService.record(server, shipment.supplyOrderId(), "DELIVERED",
                            shipment.buyer(), shipment.supplierUuid(), shipment.itemId(), shipment.quantity(),
                            shipmentValue(shipment));
                }
                deliveries.record(new LogisticsDeliverySavedData.Delivery(shipment.id(), shipment.buyer(),
                        shipment.itemId(), shipment.quantity(), now));
            }
            data.remove(shipment.id());
        }
    }

    private static long actualLoss(LogisticsSavedData.Shipment shipment, long fallback) {
        if (shipment.unitPrice() <= 0L) {
            return fallback;
        }
        try {
            return Math.multiplyExact((long) shipment.quantity(), shipment.unitPrice());
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }

    private static long shipmentValue(LogisticsSavedData.Shipment shipment) {
        if (shipment.unitPrice() <= 0L) return 0L;
        try {
            return Math.multiplyExact((long) shipment.quantity(), shipment.unitPrice());
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }

    private static Item parseItem(String itemId) {
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            return item == null || item == Items.AIR ? null : item;
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
