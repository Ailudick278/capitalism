package com.ailudick.capitalismmod.risk;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.bank.BankAccount;
import com.ailudick.capitalismmod.bank.BankAccountHelper;
import com.ailudick.capitalismmod.bank.BankExposureSavedData;
import com.ailudick.capitalismmod.bond.BondHolding;
import com.ailudick.capitalismmod.bond.BondEconomics;
import com.ailudick.capitalismmod.bond.BondSavedData;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.loan.CompanyLoan;
import com.ailudick.capitalismmod.loan.CompanyLoanSavedData;
import com.ailudick.capitalismmod.loan.PeerLoan;
import com.ailudick.capitalismmod.loan.PeerLoanSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Calculates normalized financial exposure once per simulated day. */
public final class FinancialRiskService {
    private FinancialRiskService() {}

    public static FinancialRiskSnapshot settleDaily(MinecraftServer server, long day) {
        FinancialRiskSavedData data = FinancialRiskSavedData.get(server);
        FinancialRiskSnapshot latest = data.latest();
        if (latest != null && latest.day() >= day) return latest;
        long company = 0L, peer = 0L, bank = 0L, bonds = 0L, overdue = 0L;
        int overdueCount = 0;
        for (CompanyLoan loan : CompanyLoanSavedData.get(server).loans()) {
            long liability = toBase(server, add(loan.principal(), loan.interestDue()), loan.currencyId());
            company = add(company, liability);
            if (loan.isOverdue()) { overdue = add(overdue, liability); overdueCount++; }
        }
        for (PeerLoan loan : PeerLoanSavedData.get(server).loans()) {
            long liability = toBase(server, add(loan.principal(), loan.interestDue()), loan.currencyId());
            peer = add(peer, liability);
            if (loan.isOverdue()) { overdue = add(overdue, liability); overdueCount++; }
        }
        BankExposureSavedData exposure = BankExposureSavedData.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) exposure.sync(player);
        for (BankExposureSavedData.Exposure value : exposure.exposures().values()) {
            bank = add(bank, value.loanDebtMinor());
            overdue = add(overdue, value.overdueDebtMinor());
            overdueCount += value.overdueAccounts();
        }
        for (BondHolding holding : BondSavedData.get(server).holdings()) {
            long coupon = BondEconomics.maturityCoupon(holding.faceValue(), holding.ratePerYear(), holding.totalDays());
            bonds = add(bonds, toBase(server, add(holding.faceValue(), coupon), Currencies.USD.id()));
        }
        long total = add(add(add(company, peer), bank), bonds);
        int share = total <= 0L ? 0 : (int) (overdue > Long.MAX_VALUE / 10000L
                ? 10000L : Math.min(10000L, overdue * 10000L / total));
        FinancialRiskSnapshot snapshot = new FinancialRiskSnapshot(day, company, peer, bank, bonds, overdue, overdueCount, share);
        data.record(snapshot);
        return snapshot;
    }

    private static long toBase(MinecraftServer server, long major, String currencyId) {
        if (major <= 0L || !Currencies.exists(currencyId)) return 0L;
        long minor = Money.toMinorSaturated(major);
        return ExchangeRates.convert(minor, Currencies.byId(currencyId), Config.defaultCurrency());
    }

    private static long toBaseMinor(long minor, String currencyId) {
        if (minor <= 0L || !Currencies.exists(currencyId)) return 0L;
        return ExchangeRates.convert(minor, Currencies.byId(currencyId), Config.defaultCurrency());
    }

    private static long add(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }
}
