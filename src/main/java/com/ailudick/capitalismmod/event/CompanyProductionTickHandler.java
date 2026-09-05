package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanyProductionSavedData;
import com.ailudick.capitalismmod.company.CompanySavedData;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

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
            for (int i = 0; i < cycles; i++) {
                int capacity = Math.max(1, CompanyHelper.parallelCapacity(server, company));
                boolean anySuccess = false;
                for (int batch = 0; batch < capacity; batch++) {
                    if (CompanyHelper.runProductionCycle(server, company)) {
                        successful = increment(Math.max(0L, successful));
                        anySuccess = true;
                    } else {
                        break;
                    }
                }
                if (!anySuccess) failed = increment(failed);
            }
            // Advance to now even when the catch-up cap is reached. A failed
            // attempt means the factory was idle, not that it stores infinite
            // unpaid production time for later exploitation.
            production.put(new CompanyProductionSavedData.ProductionState(
                    company.companyId(), now, successful, failed));
        }
    }

    private static long increment(long value) {
        return value == Long.MAX_VALUE ? value : value + 1L;
    }
}
