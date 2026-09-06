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
import com.ailudick.capitalismmod.economy.FinancialSettlementJournalSavedData;

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
        String issuanceId = "bond-issuance:" + UUID.randomUUID();
        var risk = FinancialRiskSavedData.get(player.getServer()).latest();
        int overdueShare = risk == null ? 0 : risk.overdueShareBasisPoints();
        double rate = MonetaryPolicyEconomics.adjustedAnnualRate(Config.BOND_RATE_PER_YEAR.get(),
                GovernmentPolicySavedData.get(player.getServer()).policyRateBasisPoints())
                + FinancialRiskPolicy.bondLiquidityPremium(overdueShare);
        int days = Config.BOND_MATURITY_DAYS.get();
        if (total <= 0 || totalMinor < 0) {
            return false;
        }
        FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(player.getServer());
        long now = player.getServer().overworld().getGameTime();
        BondIssuanceSavedData issuanceData = BondIssuanceSavedData.get(player.getServer());
        BondIssuanceSavedData.Issuance pending = new BondIssuanceSavedData.Issuance(
                issuanceId, player.getUUID(), count, faceValue, rate, days, false, false, false);
        issuanceData.add(pending);
        journal.markStarted(issuanceId, "bond", "payment", totalMinor, now);
        String paymentReference = issuanceId + ":buyer-payment";
        if (!EconomyHelper.tryPayWithReference(player, Currencies.USD, totalMinor, paymentReference)) {
            issuanceData.remove(issuanceId);
            return false;
        }
        issuanceData.replace(new BondIssuanceSavedData.Issuance(
                issuanceId, player.getUUID(), count, faceValue, rate, days, true, false, false));
        journal.markCompleted(issuanceId, "bond", "payment", totalMinor, now);
        long treasuryProceeds = com.ailudick.capitalismmod.currency.ExchangeRates.convert(
                totalMinor, Currencies.USD, Config.defaultCurrency());
        if (!GovernmentPolicySavedData.get(player.getServer()).depositOnce(treasuryProceeds, issuanceId)) {
            return false;
        }
        issuanceData.markFunded(issuanceId);
        journal.markCompleted(issuanceId, "bond", "treasury", treasuryProceeds, now);
        createHoldings(player.getServer(), new BondIssuanceSavedData.Issuance(
                issuanceId, player.getUUID(), count, faceValue, rate, days, true, true, false));
        return true;
    }

    /** Completes all funded issuance batches using deterministic holding IDs. */
    public static int recoverIssuances(MinecraftServer server) {
        int recovered = 0;
        BondIssuanceSavedData issuanceData = BondIssuanceSavedData.get(server);
        GovernmentPolicySavedData policy = GovernmentPolicySavedData.get(server);
        for (BondIssuanceSavedData.Issuance issuance : issuanceData.pendingPayment()) {
            if (!issuance.paymentConfirmed()) {
                ServerPlayer holder = server.getPlayerList().getPlayer(issuance.holder());
                long amountMinor;
                try {
                    amountMinor = Money.toMinor(Math.multiplyExact(issuance.faceValue(), issuance.count()));
                } catch (ArithmeticException exception) {
                    continue;
                }
                if (holder == null || !EconomyHelper.tryPayWithReference(holder, Currencies.USD,
                        amountMinor, issuance.id() + ":buyer-payment")) continue;
                issuanceData.replace(new BondIssuanceSavedData.Issuance(issuance.id(), issuance.holder(), issuance.count(),
                        issuance.faceValue(), issuance.ratePerYear(), issuance.days(), true, false, false));
            }
            long proceeds;
            try {
                proceeds = ExchangeRates.convert(Money.toMinor(
                        Math.multiplyExact(issuance.faceValue(), issuance.count())),
                        Currencies.USD, Config.defaultCurrency());
            } catch (ArithmeticException exception) {
                continue;
            }
            String source = issuance.id();
            if (!policy.hasDeposit(source) && !policy.depositOnce(proceeds, source)) continue;
            issuanceData.markFunded(source);
            FinancialSettlementJournalSavedData.get(server).markCompleted(source, "bond", "treasury",
                    proceeds, server.overworld().getGameTime());
            recovered++;
        }
        for (BondIssuanceSavedData.Issuance issuance : BondIssuanceSavedData.get(server).pendingFunded()) {
            createHoldings(server, issuance);
            FinancialSettlementJournalSavedData.get(server).markCompleted(issuance.id(), "bond", "holdings",
                    issuance.count(), server.overworld().getGameTime());
            recovered++;
        }
        return recovered;
    }

    private static void createHoldings(MinecraftServer server, BondIssuanceSavedData.Issuance issuance) {
        BondSavedData data = BondSavedData.get(server);
        for (int i = 0; i < issuance.count(); i++) {
            String holdingId = issuance.id() + ":" + i;
            if (data.findHolding(holdingId) == null) {
                data.addHolding(new BondHolding(holdingId, issuance.holder(), issuance.faceValue(),
                        issuance.ratePerYear(), issuance.days(), issuance.days()));
            }
        }
        BondIssuanceSavedData.get(server).markHoldingsCreated(issuance.id());
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
        String spendingId = "bond-redeem:" + holdingId;
        FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(player.getServer());
        long now = player.getServer().overworld().getGameTime();
        journal.markStarted(holdingId, "bond", "redemption-funding", basePayout, now);
        GovernmentPolicySavedData policy = GovernmentPolicySavedData.get(player.getServer());
        if (!policy.hasSpending(spendingId) && !policy.spend(
                "bond-holder:" + player.getUUID(), player.getServer().overworld().getGameTime()
                / PerpetualCalendar.TICKS_PER_DAY, basePayout, spendingId)) return false;
        journal.markCompleted(holdingId, "bond", "redemption-funding", basePayout, now);
        String payoutSource = "bond-payout:" + holdingId;
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(player.getServer());
        if (!mailbox.hasCreditSource(payoutSource)
                && !mailbox.creditMoneyOnce(player.getUUID(), Currencies.USD.id(), payoutMinor, payoutSource)) return false;
        journal.markCompleted(holdingId, "bond", "redemption-payout", payoutMinor, now);
        settlements.record(holdingId);
        data.removeHolding(holdingId);
        mailbox.redeemMoneyOnly(player);
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
            long coupon = BondEconomics.maturityCoupon(holding.faceValue(), holding.ratePerYear(), holding.totalDays());
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
            String spendingId = "bond-maturity:" + holding.id();
            FinancialSettlementJournalSavedData journal = FinancialSettlementJournalSavedData.get(server);
            long now = server.overworld().getGameTime();
            journal.markStarted(holding.id(), "bond", "maturity-funding", basePayout, now);
            GovernmentPolicySavedData policy = GovernmentPolicySavedData.get(server);
            if (!policy.hasSpending(spendingId) && !policy.spend(
                    "bond-holder:" + holding.holder(), settlementDay, basePayout, spendingId)) continue;
            journal.markCompleted(holding.id(), "bond", "maturity-funding", basePayout, now);
            String payoutSource = "bond-payout:" + holding.id();
            MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
            if (!mailbox.hasCreditSource(payoutSource)
                    && !mailbox.creditMoneyOnce(holding.holder(), Currencies.USD.id(), payoutMinor, payoutSource)) continue;
            journal.markCompleted(holding.id(), "bond", "maturity-payout", payoutMinor, now);
            settlements.record(holding.id());
            data.removeHolding(holding.id());
            ServerPlayer holder = server.getPlayerList().getPlayer(holding.holder());
            if (holder != null) mailbox.redeemMoneyOnly(holder);
        }
    }
}
