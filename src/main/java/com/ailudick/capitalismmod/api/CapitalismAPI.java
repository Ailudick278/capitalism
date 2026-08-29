package com.ailudick.capitalismmod.api;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.world.entity.player.Player;

/**
 * Public API for other mods to interact with the economy.
 * Call these from a soft dependency (compileOnly + optional).
 */
public final class CapitalismAPI {
    private CapitalismAPI() {
    }

    public static boolean hasCurrency(String currencyId) {
        return Currencies.exists(currencyId);
    }

    public static long getBalance(Player player, String currencyId) {
        if (!hasCurrency(currencyId)) {
            return 0L;
        }
        return EconomyHelper.getBalance(player, Currencies.byId(currencyId)) / Money.MINOR_UNITS_PER_UNIT;
    }

    public static void addMoney(Player player, String currencyId, long amount) {
        if (!hasCurrency(currencyId) || amount == 0) {
            return;
        }
        EconomyHelper.giveMoney(player, Currencies.byId(currencyId), Money.toMinor(amount));
    }

    /** Returns true if the player had enough balance and the amount was deducted. */
    public static boolean trySpend(Player player, String currencyId, long amount) {
        if (!hasCurrency(currencyId) || amount <= 0) {
            return false;
        }
        return EconomyHelper.tryPay(player, Currencies.byId(currencyId), Money.toMinor(amount));
    }
}
