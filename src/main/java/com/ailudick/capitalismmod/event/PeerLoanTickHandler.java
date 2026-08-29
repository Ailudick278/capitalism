package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.loan.PeerLoan;
import com.ailudick.capitalismmod.loan.PeerLoanSavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;

/**
 * Ticks peer-loan maturities once per Minecraft day.
 */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public class PeerLoanTickHandler {
    private static int dayCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        dayCounter++;
        if (dayCounter < 24000) {
            return;
        }
        dayCounter = 0;
        PeerLoanSavedData data = PeerLoanSavedData.get(event.getServer());
        for (PeerLoan loan : new ArrayList<>(data.loans())) {
            data.replaceLoan(loan.withDaysRemaining(loan.daysRemaining() - 1));
        }
        data.setDirty();
    }
}
