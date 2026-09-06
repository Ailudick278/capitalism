package com.ailudick.capitalismmod.economy;

import com.ailudick.capitalismmod.auction.AuctionMarket;
import com.ailudick.capitalismmod.currency.CurrencyExchangeService;
import com.ailudick.capitalismmod.futures.FuturesMarket;
import com.ailudick.capitalismmod.market.CommodityMarket;
import com.ailudick.capitalismmod.market.LogisticsLossService;
import com.ailudick.capitalismmod.stock.StockMarket;
import com.ailudick.capitalismmod.supply.SupplyMarket;
import net.minecraft.server.MinecraftServer;

/** Coordinates server-safe, idempotent recovery passes across economic systems. */
public final class EconomicRecoveryService {
    private EconomicRecoveryService() {}

    /**
     * Replays only operations that do not require a live player inventory.
     * Player-bound wallet and bank operations remain on the login path.
     */
    public static int recoverServerSafe(MinecraftServer server) {
        int recovered = 0;
        recovered += SupplyMarket.recoverPendingOrderIntents(server);
        recovered += LogisticsLossService.recoverSupplyCompensations(server);
        recovered += FuturesMarket.recoverPendingOpenPositions(server);
        recovered += CommodityMarket.recoverPendingTrades(server);
        recovered += StockMarket.recoverPendingTrades(server);
        recovered += AuctionMarket.recoverListingIntents(server);
        recovered += CurrencyExchangeService.recover(server);
        recovered += PlayerTransferService.recover(server);
        return recovered;
    }
}
