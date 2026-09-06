package com.ailudick.capitalismmod.government;

import com.ailudick.capitalismmod.bank.BankExposureSavedData;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.population.PopulationSavedData;
import net.minecraft.server.MinecraftServer;

/** Calculates transparent M1-style private money and bank-credit aggregates. */
public final class MoneySupplyService {
    private MoneySupplyService() {}

    public static MoneySupplySavedData.Snapshot settleDaily(MinecraftServer server, long day) {
        MoneySupplySavedData data = MoneySupplySavedData.get(server);
        MoneySupplySavedData.Snapshot latest = data.latest();
        if (latest != null && latest.day() >= day) return latest;
        long household = 0L;
        for (var h : PopulationSavedData.get(server).households()) household = add(household, h.cashMinor());
        long company = 0L;
        for (var c : CompanySavedData.get(server).companies().values()) company = add(company, c.treasuryOf(Currencies.USD.id()));
        BankExposureSavedData exposure = BankExposureSavedData.get(server);
        for (var player : server.getPlayerList().getPlayers()) exposure.sync(player);
        long deposits = 0L, credit = 0L;
        for (var value : exposure.exposures().values()) {
            deposits = add(deposits, value.depositsMinor());
            credit = add(credit, value.loanDebtMinor());
        }
        long privateMoney = add(add(household, company), deposits);
        MoneySupplySavedData.Snapshot snapshot = new MoneySupplySavedData.Snapshot(day, household, company,
                deposits, GovernmentPolicySavedData.get(server).treasuryMinor(), privateMoney, credit);
        data.record(snapshot);
        return snapshot;
    }

    private static long add(long left, long right) {
        if (right <= 0L) return left;
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }
}
