package com.ailudick.capitalismmod.bank;

import com.ailudick.capitalismmod.Config;
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
        long settlementTick = com.ailudick.capitalismmod.calendar.PerpetualCalendar.ticksForDays(day);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (BankAccount account : BankAccountHelper.getAccounts(player).values()) {
                for (BankTransaction transaction : account.transactions()) {
                    if (transaction.occurredAt() != settlementTick || !"interest".equals(transaction.type())) continue;
                    if (transaction.amount() < 0L) income = add(income, -transaction.amount());
                    else expense = add(expense, transaction.amount());
                }
            }
        }
        capital.applyDailyResult(day, income, expense);
        return capital;
    }

    private static long add(long left, long right) {
        if (right <= 0L) return left;
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }
}
