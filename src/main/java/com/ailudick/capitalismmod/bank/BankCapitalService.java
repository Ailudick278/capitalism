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
        long overdueDebt = 0L;
        long settlementTick = com.ailudick.capitalismmod.calendar.PerpetualCalendar.ticksForDays(day);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BankExposureSavedData.get(server).sync(player);
            for (BankAccount account : BankAccountHelper.getAccounts(player).values()) {
                for (BankTransaction transaction : account.transactions()) {
                    if (transaction.occurredAt() != settlementTick) continue;
                    String source = transactionSource(account, transaction);
                    if ("interest".equals(transaction.type())) {
                        if (transaction.amount() < 0L) {
                            capital.applyTransactionOnce(source,
                                    BankCapitalEconomics.positiveMagnitude(transaction.amount()), 0L);
                        } else if (transaction.amount() > 0L) {
                            capital.applyTransactionOnce(source, 0L, transaction.amount());
                        }
                    } else if ("transfer_fee".equals(transaction.type()) && transaction.amount() < 0L) {
                        capital.applyTransactionOnce(source,
                                BankCapitalEconomics.positiveMagnitude(transaction.amount()), 0L);
                    }
                }
            }
        }
        for (BankExposureSavedData.Exposure exposure : BankExposureSavedData.get(server).exposures().values()) {
            overdueDebt = add(overdueDebt, exposure.overdueDebtMinor());
        }
        long provisionDelta = capital.adjustLossProvision(
                BankCapitalEconomics.lossProvisionTarget(overdueDebt));
        long provisionIncome = provisionDelta < 0L ? positiveMagnitude(provisionDelta) : 0L;
        long provisionExpense = provisionDelta > 0L ? provisionDelta : 0L;
        capital.applyDailyResult(day, provisionIncome, provisionExpense);
        return capital;
    }

    /** Reconciles interest and fee transactions created while a player was offline. */
    public static int reconcilePlayerTransactions(ServerPlayer player) {
        if (player == null || player.getServer() == null) return 0;
        BankCapitalSavedData capital = BankCapitalSavedData.get(player.getServer());
        if (!capital.initialized()) capital.initialize(Config.BANK_INITIAL_CAPITAL_MINOR.get());
        int reconciled = 0;
        for (BankAccount account : BankAccountHelper.getAccounts(player).values()) {
            for (BankTransaction transaction : account.transactions()) {
                long income = 0L;
                if ("interest".equals(transaction.type()) && transaction.amount() < 0L) {
                    income = BankCapitalEconomics.positiveMagnitude(transaction.amount());
                } else if ("transfer_fee".equals(transaction.type()) && transaction.amount() < 0L) {
                    income = BankCapitalEconomics.positiveMagnitude(transaction.amount());
                }
                if (income > 0L && capital.applyTransactionOnce(transactionSource(account, transaction), income, 0L)) {
                    reconciled++;
                }
            }
        }
        return reconciled;
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

    private static long positiveMagnitude(long value) {
        return value == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(value);
    }

    private static String transactionSource(BankAccount account, BankTransaction transaction) {
        return "bank-transaction:" + account.id() + ":" + transaction.occurredAt()
                + ":" + transaction.type() + ":" + transaction.currencyId() + ":" + transaction.amount()
                + ":" + transaction.reference() + ":" + transaction.counterparty();
    }
}
