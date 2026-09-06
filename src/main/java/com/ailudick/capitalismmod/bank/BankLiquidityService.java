package com.ailudick.capitalismmod.bank;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.risk.FinancialCrisisSavedData;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.risk.FinancialRiskSavedData;
import net.minecraft.server.MinecraftServer;

/** Aggregates online bank exposure and limits crisis-period withdrawals to 10% of deposits per day. */
public final class BankLiquidityService {
    private BankLiquidityService() {}

    public static BankLiquiditySnapshot settleDaily(MinecraftServer server, long day) {
        BankLiquiditySavedData data = BankLiquiditySavedData.get(server);
        BankLiquiditySnapshot latest = data.latest();
        if (latest != null && latest.day() >= day) return latest;
        BankExposureSavedData exposure = BankExposureSavedData.get(server);
        for (var player : server.getPlayerList().getPlayers()) exposure.sync(player);
        long deposits = 0L, loans = 0L;
        for (BankExposureSavedData.Exposure value : exposure.exposures().values()) {
            deposits = add(deposits, value.depositsMinor());
            loans = add(loans, value.loanDebtMinor());
        }
        long withdrawn = data.withdrawalsForAssessment(day);
        boolean pressure = BankLiquidityEconomics.solvencyStress(deposits, loans)
                || BankLiquidityEconomics.withdrawalRunStress(deposits, withdrawn);
        boolean limited = FinancialCrisisSavedData.get(server).active() || pressure;
        BankLiquiditySnapshot snapshot = new BankLiquiditySnapshot(day, deposits, loans, withdrawn, limited);
        data.record(snapshot); return snapshot;
    }

    public static boolean authorizeWithdrawal(MinecraftServer server, String currencyId, long amount) {
        if (server == null || amount <= 0L) return false;
        if (!FinancialCrisisSavedData.get(server).active()) return true;
        BankLiquiditySnapshot snapshot = settleDaily(server,
                server.overworld().getGameTime() / PerpetualCalendar.TICKS_PER_DAY);
        long baseAmount = toBase(amount, currencyId);
        long limit = BankLiquidityEconomics.crisisWithdrawalLimit(snapshot.depositsMinor());
        return BankLiquiditySavedData.get(server).reserveWithdrawal(snapshot.day(), baseAmount, limit);
    }

    public static boolean authorizeLoan(MinecraftServer server, String currencyId, long amount) {
        if (server == null || amount <= 0L) return false;
        if (!FinancialCrisisSavedData.get(server).active()) return true;
        BankLiquiditySnapshot snapshot = settleDaily(server,
                server.overworld().getGameTime() / PerpetualCalendar.TICKS_PER_DAY);
        long baseAmount = toBase(amount, currencyId);
        var risk = FinancialRiskSavedData.get(server).latest();
        int overdueShare = risk == null ? 0 : risk.overdueShareBasisPoints();
        long capacity = BankLiquidityEconomics.riskAdjustedLoanCapacity(snapshot.depositsMinor(), overdueShare);
        return baseAmount > 0L && snapshot.loanDebtMinor() <= capacity
                && baseAmount <= capacity - snapshot.loanDebtMinor();
    }

    private static long toBase(long amount, String currencyId) {
        if (amount <= 0L || !Currencies.exists(currencyId)) return 0L;
        return ExchangeRates.convert(amount, Currencies.byId(currencyId), Config.defaultCurrency());
    }
    private static long add(long left, long right) { return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right; }
}
