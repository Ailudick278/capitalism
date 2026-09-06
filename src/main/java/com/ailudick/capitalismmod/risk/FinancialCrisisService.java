package com.ailudick.capitalismmod.risk;

import net.minecraft.server.MinecraftServer;

/** Updates crisis state from the latest risk snapshot using entry/recovery hysteresis. */
public final class FinancialCrisisService {
    private FinancialCrisisService() {}

    public static boolean update(MinecraftServer server, long day) {
        FinancialRiskSnapshot risk = FinancialRiskSavedData.get(server).latest();
        if (risk == null) return false;
        FinancialCrisisSavedData crisis = FinancialCrisisSavedData.get(server);
        if (!crisis.active() && FinancialRiskPolicy.crisisTriggered(risk.overdueShareBasisPoints())) {
            crisis.enter(day); return true;
        }
        if (crisis.active() && FinancialRiskPolicy.crisisRecovered(risk.overdueShareBasisPoints())) {
            crisis.recover(day); return true;
        }
        return crisis.active();
    }
}
