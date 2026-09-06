package com.ailudick.capitalismmod.risk;

import net.minecraft.server.MinecraftServer;
import com.ailudick.capitalismmod.bank.BankLiquidityEconomics;
import com.ailudick.capitalismmod.bank.BankLiquiditySavedData;
import com.ailudick.capitalismmod.economy.expansion.EconomicEventService;

/** Updates crisis state from the latest risk snapshot using entry/recovery hysteresis. */
public final class FinancialCrisisService {
    private FinancialCrisisService() {}

    public static boolean update(MinecraftServer server, long day) {
        FinancialRiskSnapshot risk = FinancialRiskSavedData.get(server).latest();
        if (risk == null) return false;
        FinancialCrisisSavedData crisis = FinancialCrisisSavedData.get(server);
        var liquidity = BankLiquiditySavedData.get(server).latest();
        boolean bankStress = liquidity != null
                && (BankLiquidityEconomics.solvencyStress(liquidity.depositsMinor(), liquidity.loanDebtMinor())
                || BankLiquidityEconomics.withdrawalRunStress(liquidity.depositsMinor(), liquidity.withdrawnMinor()));
        boolean severe = FinancialRiskPolicy.crisisTriggered(risk.overdueShareBasisPoints()) || bankStress;
        boolean recovered = FinancialRiskPolicy.crisisRecovered(risk.overdueShareBasisPoints()) && !bankStress;
        if (!crisis.active() && severe) {
            crisis.enter(day);
            EconomicEventService.addFinancialCrisisSignal(server, day, server.overworld().getGameTime());
            return true;
        }
        if (crisis.active() && recovered) {
            boolean released = crisis.observeRecovery(day);
            if (released && !crisis.active()) EconomicEventService.resolveFinancialCrisisSignals(server);
            return crisis.active();
        }
        if (crisis.active()) crisis.resetRecoveryObservation();
        return crisis.active();
    }
}
