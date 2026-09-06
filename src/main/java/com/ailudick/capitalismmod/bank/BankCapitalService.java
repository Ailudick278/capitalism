package com.ailudick.capitalismmod.bank;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.government.GovernmentPolicySavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Reconciles bank operating interest into the persistent equity ledger. */
public final class BankCapitalService {
    private BankCapitalService() {}

    public static BankCapitalSavedData settleDaily(MinecraftServer server, long day) {
        BankCapitalSavedData capital = BankCapitalSavedData.get(server);
        if (!capital.initialized()) capital.initialize(Config.BANK_INITIAL_CAPITAL_MINOR.get());
        if (capital.lastSettlementDay() >= day) return capital;
        long income = 0L;
        long expense = 0L;
        long overdueDebt = 0L;
        long settlementTick = com.ailudick.capitalismmod.calendar.PerpetualCalendar.ticksForDays(day);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BankExposureSavedData.get(server).sync(player);
            for (BankAccount account : BankAccountHelper.getAccounts(player).values()) {
                for (BankTransaction transaction : account.transactions()) {
                    if (transaction.occurredAt() != settlementTick) continue;
                    if ("interest".equals(transaction.type())) {
                        if (transaction.amount() < 0L) {
                            income = add(income, BankCapitalEconomics.positiveMagnitude(transaction.amount()));
                        }
                        else expense = add(expense, transaction.amount());
                    } else if ("transfer_fee".equals(transaction.type()) && transaction.amount() < 0L) {
                        income = add(income, BankCapitalEconomics.positiveMagnitude(transaction.amount()));
                    }
                }
            }
        }
        for (BankExposureSavedData.Exposure exposure : BankExposureSavedData.get(server).exposures().values()) {
            overdueDebt = add(overdueDebt, exposure.overdueDebtMinor());
        }
        long provisionDelta = capital.adjustLossProvision(
                BankCapitalEconomics.lossProvisionTarget(overdueDebt));
        if (provisionDelta >= 0L) expense = add(expense, provisionDelta);
        else income = add(income, -provisionDelta);
        capital.applyDailyResult(day, income, expense);
        return capital;
    }

    /** Transfers fiscal funds into bank equity with a retryable two-ledger receipt. */
    public static boolean injectFromTreasury(MinecraftServer server, long day, long amountMinor,
                                             String sourceId) {
        if (server == null || amountMinor <= 0L || sourceId == null || sourceId.isBlank()) return false;
        BankCapitalSavedData capital = BankCapitalSavedData.get(server);
        if (!capital.initialized()) capital.initialize(Config.BANK_INITIAL_CAPITAL_MINOR.get());
        if (capital.hasInjection(sourceId)) return true;
        GovernmentPolicySavedData government = GovernmentPolicySavedData.get(server);
        String spendingId = "bank-capital-injection:" + sourceId;
        if (!government.hasSpending(spendingId)
                && !government.spend("bank-capital-injection", day, amountMinor, spendingId)) return false;
        return capital.injectOnce(amountMinor, sourceId) || capital.hasInjection(sourceId);
    }

    private static long add(long left, long right) {
        if (right <= 0L) return left;
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }
}
