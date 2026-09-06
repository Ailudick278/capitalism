package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanyProductionSavedData;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.company.CompanyEconomy;
import com.ailudick.capitalismmod.company.ProductionRecipe;
import com.ailudick.capitalismmod.company.ProductionCycleIdentity;
import com.ailudick.capitalismmod.company.ProductionPlanningEconomics;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import com.ailudick.capitalismmod.market.InventoryOwner;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.supply.PurchaseOrder;
import com.ailudick.capitalismmod.supply.SupplyMarketSavedData;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;

/** Runs persisted company production attempts for both online and offline owners. */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class CompanyProductionTickHandler {
    private static final int CHECK_INTERVAL = 20;
    private static int tickCounter;

    private CompanyProductionTickHandler() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!Config.COMPANY_PRODUCTION_ENABLED.get() || ++tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        long cycleTicks = Config.COMPANY_PRODUCTION_CYCLE_TICKS.get();
        int maxCatchup = Config.COMPANY_PRODUCTION_MAX_CATCHUP_CYCLES.get();
        CompanyProductionSavedData production = CompanyProductionSavedData.get(server);

        for (Company company : CompanySavedData.get(server).companies().values()) {
            CompanyProductionSavedData.ProductionState state = production.get(company.companyId());
            if (state == null) {
                production.put(new CompanyProductionSavedData.ProductionState(
                        company.companyId(), now, 0L, 0L));
                continue;
            }
            if (now <= state.lastProcessedTick()) continue;
            long elapsed = now - state.lastProcessedTick();
            long dueCycles = elapsed / cycleTicks;
            if (dueCycles <= 0L) continue;

            int cycles = (int) Math.min((long) maxCatchup, dueCycles);
            long successful = state.successfulCycles();
            long failed = state.failedCycles();
            Map<String, Long> failureReasons = state.failureReasons();
            ProductionRecipe recipe = CompanyEconomy.recipe(company);
            for (int i = 0; i < cycles; i++) {
                long cycleTick = state.lastProcessedTick() + (long) (i + 1) * cycleTicks;
                if (recipe != null && !recipe.isService() && !shouldRun(company, recipe, server)) continue;
                int capacity = Math.max(1, CompanyHelper.parallelCapacity(server, company));
                boolean anySuccess = false;
                for (int batch = 0; batch < capacity; batch++) {
                    String cycleKey = ProductionCycleIdentity.key(company.companyId(), cycleTick, batch);
                    CompanyHelper.ProductionCycleResult result = CompanyHelper.runProductionCycleResult(server, company, cycleKey);
                    if (result.success()) {
                        successful = increment(Math.max(0L, successful));
                        anySuccess = true;
                    } else {
                        failureReasons = CompanyProductionSavedData.incrementReason(failureReasons,
                                result.failureReason());
                        break;
                    }
                }
                if (!anySuccess) failed = increment(failed);
            }
            // Advance to now even when the catch-up cap is reached. A failed
            // attempt means the factory was idle, not that it stores infinite
            // unpaid production time for later exploitation.
            production.put(new CompanyProductionSavedData.ProductionState(
                    company.companyId(), now, successful, failed, failureReasons));
        }
    }

    private static boolean shouldRun(Company company, ProductionRecipe recipe, MinecraftServer server) {
        int backlog = 0;
        int inventory = 0;
        long marketPrice = 0L;
        long fundamental = 0L;
        WarehouseSavedData warehouse = WarehouseSavedData.get(server);
        CommoditySavedData commodities = CommoditySavedData.get(server);
        SupplyMarketSavedData supply = SupplyMarketSavedData.get(server);
        InventoryOwner owner = InventoryOwner.company(company.companyId());
        for (Map.Entry<String, Integer> output : recipe.outputs().entrySet()) {
            int units = Math.max(0, output.getValue());
            inventory = saturatingInt(inventory, warehouse.count(owner, output.getKey()));
            for (PurchaseOrder order : supply.orders()) {
                if (order.supplierUuid().equals(company.ownerUuid())
                        && order.companyName().equals(company.name())
                        && order.itemId().equals(output.getKey())) {
                    backlog = saturatingInt(backlog, order.remaining());
                }
            }
            long price = commodities.price(output.getKey());
            long base = commodities.fundamental(output.getKey());
            marketPrice = Math.max(marketPrice, price);
            fundamental = Math.max(fundamental, base);
            if (units <= 0) continue;
        }
        int batchOutput = recipe.outputs().values().stream().mapToInt(value -> Math.max(0, value)).sum();
        return ProductionPlanningEconomics.shouldRun(backlog, inventory, batchOutput, marketPrice,
                fundamental, company.treasuryOf(com.ailudick.capitalismmod.currency.Currencies.USD.id()),
                recipe.energyCost() + recipe.maintenanceCost());
    }

    private static int saturatingInt(int left, int right) {
        long sum = (long) Math.max(0, left) + Math.max(0, right);
        return (int) Math.min(Integer.MAX_VALUE, sum);
    }

    private static long increment(long value) {
        return value == Long.MAX_VALUE ? value : value + 1L;
    }
}
