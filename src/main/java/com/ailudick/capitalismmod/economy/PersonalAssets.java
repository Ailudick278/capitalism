package com.ailudick.capitalismmod.economy;

import com.ailudick.capitalismmod.bond.BondSavedData;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.futures.FuturesSavedData;
import com.ailudick.capitalismmod.market.WarehouseSavedData;
import com.ailudick.capitalismmod.stock.StockOrder;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;

/** Calculates a read-only USD estimate for the player's major asset categories. */
public final class PersonalAssets {
    private PersonalAssets() {
    }

    public static Map<String, Long> estimate(MinecraftServer server, ServerPlayer player) {
        EconomySavedData economy = EconomySavedData.get(server);
        Map<String, Long> result = new LinkedHashMap<>();
        long cash = 0L;
        for (var currency : Currencies.ALL) {
            cash = safeAdd(cash, ExchangeRates.convert(EconomyHelper.getBalance(player, currency), currency, Currencies.USD));
        }
        result.put("cash", cash);

        long stocks = 0L;
        for (Map.Entry<String, Long> entry : economy.portfolio(player.getUUID()).entrySet()) {
            stocks = safeAdd(stocks, safeMajorToMinor(EconomyMath.multiply(entry.getValue(), economy.price(entry.getKey()))));
        }
        result.put("stocks", stocks);

        long commodities = 0L;
        for (Map.Entry<String, Integer> entry : WarehouseSavedData.get(server).storage(player.getUUID()).entrySet()) {
            commodities = safeAdd(commodities, safeMajorToMinor(EconomyMath.multiply(entry.getValue(),
                    com.ailudick.capitalismmod.market.CommoditySavedData.get(server).price(entry.getKey()))));
        }
        result.put("commodities", commodities);

        long bonds = 0L;
        for (var holding : BondSavedData.get(server).holdings()) {
            if (holding.holder().equals(player.getUUID())) bonds = safeAdd(bonds, safeMajorToMinor(holding.faceValue()));
        }
        result.put("bonds", bonds);
        result.put("futures", safeMajorToMinor(FuturesSavedData.get(server).marginBalance(player.getUUID())));

        long total = 0L;
        for (long value : result.values()) total = safeAdd(total, value);
        result.put("total", total);
        return result;
    }

    private static long safeMajorToMinor(long value) {
        return value <= 0 ? 0L : Money.toMinor(value) < 0 ? Long.MAX_VALUE : Money.toMinor(value);
    }

    private static long safeAdd(long left, long right) {
        if (right < 0 || Long.MAX_VALUE - left < right) return Long.MAX_VALUE;
        return left + right;
    }
}
