package com.ailudick.capitalismmod.bond;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.government.GovernmentPolicySavedData;
import com.ailudick.capitalismmod.government.MonetaryPolicyEconomics;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.risk.FinancialRiskPolicy;
import com.ailudick.capitalismmod.risk.FinancialRiskSavedData;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Server-side bond market: issue government bonds, redeem early, and settle maturities.
 */
public final class BondMarket {
    private BondMarket() {
    }

    /** Buys {@code count} government bonds at face value. */
    public static boolean buyBond(ServerPlayer player, int count) {
        if (count <= 0) {
            return false;
        }
        long faceValue = Config.BOND_FACE_VALUE.get();
        long total;
        try {
            total = Math.multiplyExact(faceValue, count);
        } catch (ArithmeticException e) {
            return false;
        }
        long totalMinor = Money.toMinor(total);
        if (total <= 0 || totalMinor < 0 || !EconomyHelper.tryPay(player, Currencies.USD, totalMinor)) {
            return false;
        }
        BondSavedData data = BondSavedData.get(player.getServer());
        long treasuryProceeds = com.ailudick.capitalismmod.currency.ExchangeRates.convert(
                totalMinor, Currencies.USD, Config.defaultCurrency());
        if (!GovernmentPolicySavedData.get(player.getServer()).deposit(treasuryProceeds)) {
            EconomyHelper.giveMoney(player, Currencies.USD, totalMinor);
            return false;
        }
        var risk = FinancialRiskSavedData.get(player.getServer()).latest();
        int overdueShare = risk == null ? 0 : risk.overdueShareBasisPoints();
        double rate = MonetaryPolicyEconomics.adjustedAnnualRate(Config.BOND_RATE_PER_YEAR.get(),
                GovernmentPolicySavedData.get(player.getServer()).policyRateBasisPoints())
                + FinancialRiskPolicy.bondLiquidityPremium(overdueShare);
        int days = Config.BOND_MATURITY_DAYS.get();
        for (int i = 0; i < count; i++) {
            data.addHolding(new BondHolding(UUID.randomUUID().toString(), player.getUUID(), faceValue, rate, days, days));
        }
        return true;
    }

    /** Redeems a bond early at face value plus accrued interest. */
    public static boolean redeemBond(ServerPlayer player, String holdingId) {
        BondSavedData data = BondSavedData.get(player.getServer());
        BondSettlementSavedData settlements = BondSettlementSavedData.get(player.getServer());
        BondHolding holding = data.findHolding(holdingId);
        if (holding == null || !holding.holder().equals(player.getUUID())) {
            return false;
        }
        if (settlements.has(holdingId)) {
            data.removeHolding(holdingId);
            return true;
        }
        long payout;
        try {
            payout = Math.addExact(holding.faceValue(), holding.accruedInterest());
        } catch (ArithmeticException e) {
            return false;
        }
        long payoutMinor = Money.toMinor(payout);
        if (payout <= 0 || payoutMinor < 0) {
            return false;
        }
        long basePayout = ExchangeRates.convert(payoutMinor, Currencies.USD, Config.defaultCurrency());
        if (!GovernmentPolicySavedData.get(player.getServer()).spend(
                "bond-holder:" + player.getUUID(), player.getServer().overworld().getGameTime()
                        / PerpetualCalendar.TICKS_PER_DAY, basePayout, "bond-redeem:" + holdingId)) return false;
        EconomyHelper.giveMoney(player, Currencies.USD, payoutMinor);
        settlements.record(holdingId);
        data.removeHolding(holdingId);
        return true;
    }

    /** Ticks bond maturities; pays out full face value plus coupon at maturity. */
    public static void settleMaturity(MinecraftServer server) {
        settleMaturity(server, server.overworld().getGameTime() / PerpetualCalendar.TICKS_PER_DAY);
    }

    /** Ticks bond maturities once for the supplied settlement day. */
    public static void settleMaturity(MinecraftServer server, long settlementDay) {
        BondSavedData data = BondSavedData.get(server);
        BondSettlementSavedData settlements = BondSettlementSavedData.get(server);
        for (BondHolding holding : new ArrayList<>(data.holdings())) {
            if (settlements.has(holding.id())) {
                data.removeHolding(holding.id());
                continue;
            }
            if (holding.lastSettlementDay() >= settlementDay) continue;
            int remaining = holding.daysToMaturity() - 1;
            if (remaining > 0) {
                data.replaceHolding(holding.withDaysToMaturity(remaining).withLastSettlementDay(settlementDay));
                continue;
            }
            long coupon = (long) (holding.faceValue() * holding.ratePerYear() * holding.totalDays() / 365.0);
            long payout;
            try {
                payout = Math.addExact(holding.faceValue(), coupon);
            } catch (ArithmeticException e) {
                continue;
            }
            long payoutMinor = Money.toMinor(payout);
            if (payout <= 0 || payoutMinor < 0) {
                continue;
            }
            long basePayout = ExchangeRates.convert(payoutMinor, Currencies.USD, Config.defaultCurrency());
            if (!GovernmentPolicySavedData.get(server).spend(
                    "bond-holder:" + holding.holder(), settlementDay, basePayout,
                    "bond-maturity:" + holding.id())) continue;
            ServerPlayer holder = server.getPlayerList().getPlayer(holding.holder());
            if (holder != null) {
                EconomyHelper.giveMoney(holder, Currencies.USD, payoutMinor);
            } else {
                MarketMailboxSavedData.get(server).creditMoney(holding.holder(), "usd", payoutMinor);
            }
            settlements.record(holding.id());
            data.removeHolding(holding.id());
        }
    }
}
