package com.ailudick.capitalismmod.risk;

import net.minecraft.server.MinecraftServer;
import com.ailudick.capitalismmod.bank.BankLiquidityEconomics;
import com.ailudick.capitalismmod.bank.BankLiquiditySavedData;

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
            crisis.enter(day); return true;
        }
        if (crisis.active() && recovered) {
            crisis.observeRecovery(day); return crisis.active();
        }
        if (crisis.active()) crisis.resetRecoveryObservation();
        return crisis.active();
    }
}
