package com.ailudick.capitalismmod.futures;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.market.CommoditySavedData;
import com.ailudick.capitalismmod.util.EconomyMath;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side futures market: cash-settled, margin-based, long/short positions on
 * each commodity, with daily mark-to-market, forced liquidation, and expiry/rollover.
 *
 * <p>The futures price is quote-driven (no order book): it mean-reverts toward the
 * spot price (basis convergence, accelerating near expiry) plus net open volume.
 */
public final class FuturesMarket {
    private FuturesMarket() {
    }

    // ---- accessors ----

    public static Map<String, Long> getPrices(MinecraftServer server) {
        return FuturesSavedData.get(server).prices();
    }

    public static List<Position> getPositions(MinecraftServer server) {
        return FuturesSavedData.get(server).positions();
    }

    public static long marginBalance(MinecraftServer server, UUID playerId) {
        return FuturesSavedData.get(server).marginBalance(playerId);
    }

    /** item id -> days remaining until expiry. */
    public static Map<String, Long> getDaysToExpiry(MinecraftServer server) {
        FuturesSavedData data = FuturesSavedData.get(server);
        Map<String, Long> result = new HashMap<>();
        for (ItemStack stack : Commodities.ALL) {
            String id = Commodities.id(stack);
            result.put(id, Math.max(0, data.expiryDay(id) - data.dayCounter()));
        }
        return result;
    }

    // ---- margin account ----

    /** Moves {@code amount} USD from the wallet into the futures margin account. */
    public static boolean depositMargin(ServerPlayer player, long amount) {
        if (amount <= 0 || !EconomyHelper.tryPay(player, Currencies.USD, Money.toMinor(amount))) {
            return false;
        }
        FuturesSavedData.get(player.getServer()).addMarginBalance(player.getUUID(), amount);
        return true;
    }

    /** Moves {@code amount} USD from the futures margin account back into the wallet. */
    public static boolean withdrawMargin(ServerPlayer player, long amount) {
        if (amount <= 0) {
            return false;
        }
        FuturesSavedData data = FuturesSavedData.get(player.getServer());
        if (data.marginBalance(player.getUUID()) < amount) {
            return false;
        }
        data.addMarginBalance(player.getUUID(), -amount);
        EconomyHelper.giveMoney(player, Currencies.USD, Money.toMinor(amount));
        return true;
    }

    // ---- open / close ----

    /** Opens a long or short position of {@code quantity} at the current futures price. */
    public static boolean openPosition(ServerPlayer player, int commodityIndex, int quantity, boolean longSide) {
        if (!Commodities.isValid(commodityIndex) || quantity <= 0) {
            return false;
        }
        String itemId = Commodities.id(Commodities.get(commodityIndex));
        FuturesSavedData data = FuturesSavedData.get(player.getServer());
        long price = data.price(itemId);
        if (price <= 0) {
            return false;
        }
        long notional = EconomyMath.multiply(quantity, price);
        if (notional < 0) {
            return false;
        }
        long margin = Math.max(1L, (long) (notional * Config.FUTURES_MARGIN_RATE.get()));
        if (data.marginBalance(player.getUUID()) < margin) {
            return false;
        }

        data.addMarginBalance(player.getUUID(), -margin);
        data.addPosition(new Position(UUID.randomUUID().toString(), player.getUUID(), itemId, quantity, price, margin, longSide));
        data.addNetVolume(itemId, longSide ? quantity : -quantity);
        data.setDirty();
        return true;
    }

    /** Closes the player's own position, realizing P&L into their margin balance. */
    public static boolean closePosition(ServerPlayer player, String positionId) {
        FuturesSavedData data = FuturesSavedData.get(player.getServer());
        Position position = data.findPosition(positionId);
        if (position == null || !position.playerId().equals(player.getUUID())) {
            return false;
        }
        long price = data.price(position.itemId());
        long pnl = pnl(position, price);
        data.removePosition(positionId);
        data.addMarginBalance(player.getUUID(), safeAdd(position.margin(), pnl));
        data.addNetVolume(position.itemId(), position.longSide() ? -position.quantity() : position.quantity());
        data.setDirty();
        return true;
    }

    // ---- daily settlement ----

    /** Marks every position to market, settles expiry/rollover, and liquidates insolvent accounts. */
    public static void settleDay(MinecraftServer server) {
        FuturesSavedData data = FuturesSavedData.get(server);
        data.incrementDay();

        Set<String> ids = new LinkedHashSet<>();
        for (ItemStack stack : Commodities.ALL) {
            ids.add(Commodities.id(stack));
        }

        for (String id : ids) {
            long price = data.price(id);
            boolean expired = data.dayCounter() >= data.expiryDay(id);

            for (Position position : new ArrayList<>(data.positions())) {
                if (!position.itemId().equals(id)) {
                    continue;
                }
                long pnl = pnl(position, price);
                data.addMarginBalance(position.playerId(), pnl);
                if (expired) {
                    data.addMarginBalance(position.playerId(), position.margin());
                    data.removePosition(position.id());
                } else {
                    data.replacePosition(position.withEntryPrice(price));
                }
            }

            if (expired) {
                data.setExpiryDay(id, data.dayCounter() + Config.FUTURES_EXPIRY_DAYS.get());
            }
        }

        // Forced liquidation: positions whose account is out of margin are closed.
        for (Position position : new ArrayList<>(data.positions())) {
            if (data.marginBalance(position.playerId()) <= 0) {
                data.removePosition(position.id());
            }
        }
        data.setDirty();
    }

    // ---- price updates ----

    /** Moves the futures price toward spot (basis convergence) plus net volume. */
    public static void updatePrices(MinecraftServer server) {
        FuturesSavedData data = FuturesSavedData.get(server);
        CommoditySavedData spot = CommoditySavedData.get(server);

        Set<String> ids = new LinkedHashSet<>();
        for (ItemStack stack : Commodities.ALL) {
            ids.add(Commodities.id(stack));
        }

        for (String id : ids) {
            long futuresPrice = data.price(id);
            long spotPrice = spot.price(id);
            if (spotPrice <= 0) {
                spotPrice = futuresPrice;
            }
            long daysToExpiry = Math.max(1, data.expiryDay(id) - data.dayCounter());
            long convergeDivisor = Math.max(10L, daysToExpiry * 40L);
            long converge = (spotPrice - futuresPrice) / convergeDivisor;
            long netVolume = data.netVolume(id);
            long newPrice = Math.max(1L, futuresPrice + converge + netVolume / 10);
            data.putPrice(id, newPrice);
            data.resetNetVolume(id);
        }
        data.setDirty();
    }

    private static long pnl(Position position, long price) {
        long diff = position.longSide() ? price - position.entryPrice() : position.entryPrice() - price;
        return EconomyMath.multiply(position.quantity(), diff);
    }

    private static long safeAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        if (right < 0 && left < Long.MIN_VALUE - right) {
            return Long.MIN_VALUE;
        }
        return left + right;
    }
}
