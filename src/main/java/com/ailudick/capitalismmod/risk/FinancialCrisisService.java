package com.ailudick.capitalismmod.risk;

import net.minecraft.server.MinecraftServer;
import com.ailudick.capitalismmod.bank.BankLiquidityEconomics;
import com.ailudick.capitalismmod.bank.BankLiquiditySavedData;
import com.ailudick.capitalismmod.bank.BankLiquiditySnapshot;
import com.ailudick.capitalismmod.bank.BankCapitalSavedData;
import com.ailudick.capitalismmod.bank.BankCapitalService;
import com.ailudick.capitalismmod.bank.BankCapitalEconomics;
import com.ailudick.capitalismmod.government.GovernmentPolicySavedData;
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
            automaticRecapitalization(server, day, liquidity);
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

    /** Provides a bounded, idempotent fiscal backstop when capital falls below 8%. */
    private static void automaticRecapitalization(MinecraftServer server, long day,
                                                  BankLiquiditySnapshot liquidity) {
        if (liquidity == null || liquidity.loanDebtMinor() <= 0L) return;
        BankCapitalSavedData capital = BankCapitalSavedData.get(server);
        if (!capital.initialized()) return;
        long gap = BankCapitalEconomics.capitalGap(capital.capitalMinor(), liquidity.loanDebtMinor());
        if (gap <= 0L) return;
        GovernmentPolicySavedData government = GovernmentPolicySavedData.get(server);
        long limit = government.treasuryMinor() / 4L;
        long support = Math.min(gap, limit);
        if (support <= 0L) return;
        BankCapitalService.injectFromTreasury(server, day, support,
                "automatic-crisis:" + day + ":" + support);
    }
}
