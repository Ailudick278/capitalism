package com.ailudick.capitalismmod.bond;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

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
        long total = faceValue * count;
        if (!EconomyHelper.tryPay(player, Currencies.USD, Money.toMinor(total))) {
            return false;
        }
        BondSavedData data = BondSavedData.get(player.getServer());
        double rate = Config.BOND_RATE_PER_YEAR.get();
        int days = Config.BOND_MATURITY_DAYS.get();
        for (int i = 0; i < count; i++) {
            data.addHolding(new BondHolding(UUID.randomUUID().toString(), player.getUUID(), faceValue, rate, days, days));
        }
        return true;
    }

    /** Redeems a bond early at face value plus accrued interest. */
    public static boolean redeemBond(ServerPlayer player, String holdingId) {
        BondSavedData data = BondSavedData.get(player.getServer());
        BondHolding holding = data.findHolding(holdingId);
        if (holding == null || !holding.holder().equals(player.getUUID())) {
            return false;
        }
        long payout = holding.faceValue() + holding.accruedInterest();
        EconomyHelper.giveMoney(player, Currencies.USD, Money.toMinor(payout));
        data.removeHolding(holdingId);
        return true;
    }

    /** Ticks bond maturities; pays out full face value plus coupon at maturity. */
    public static void settleMaturity(MinecraftServer server) {
        BondSavedData data = BondSavedData.get(server);
        for (BondHolding holding : new ArrayList<>(data.holdings())) {
            int remaining = holding.daysToMaturity() - 1;
            if (remaining > 0) {
                data.replaceHolding(holding.withDaysToMaturity(remaining));
                continue;
            }
            long coupon = (long) (holding.faceValue() * holding.ratePerYear() * holding.totalDays() / 365.0);
            long payout = holding.faceValue() + coupon;
            ServerPlayer holder = server.getPlayerList().getPlayer(holding.holder());
            if (holder != null) {
                EconomyHelper.giveMoney(holder, Currencies.USD, Money.toMinor(payout));
            } else {
                MarketMailboxSavedData.get(server).creditMoney(holding.holder(), "usd", Money.toMinor(payout));
            }
            data.removeHolding(holding.id());
        }
    }
}
