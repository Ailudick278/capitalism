package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.tax.TaxLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxService;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Keeps the tax system authoritative on the server instead of relying on a
 * player opening the tax screen. Late fees and delinquency notices therefore
 * continue to advance while the affected player is offline.
 */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class TaxEnforcementTickHandler {
    private static final long CHECK_INTERVAL = 1200L;

    private TaxEnforcementTickHandler() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        if (now <= 0L || now % CHECK_INTERVAL != 0L) return;

        for (var bill : TaxLedgerSavedData.get(server).bills()) {
            if (bill.paid()) continue;
            var updated = TaxService.updateLateFee(server, bill, now);
            TaxService.processEnforcement(server, updated, now);
        }
    }
}
