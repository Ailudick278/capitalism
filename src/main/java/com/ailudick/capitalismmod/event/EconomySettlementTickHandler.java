package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.bank.BankAccountHelper;
import com.ailudick.capitalismmod.bond.BondMarket;
import com.ailudick.capitalismmod.economy.EconomySettlementSavedData;
import com.ailudick.capitalismmod.futures.FuturesMarket;
import com.ailudick.capitalismmod.loan.PeerLoan;
import com.ailudick.capitalismmod.loan.PeerLoanSavedData;
import com.ailudick.capitalismmod.market.CommodityMarket;
import com.ailudick.capitalismmod.stock.StockMarket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;

/** Runs every missed Minecraft-day settlement exactly once, including after restart. */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class EconomySettlementTickHandler {
    private static final long TICKS_PER_DAY = 24000L;

    private EconomySettlementTickHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long currentDay = event.getServer().overworld().getGameTime() / TICKS_PER_DAY;
        EconomySettlementSavedData state = EconomySettlementSavedData.get(event.getServer());
        long lastDay = state.lastSettlementDay();
        if (lastDay < 0) {
            state.setLastSettlementDay(currentDay);
            return;
        }
        while (lastDay < currentDay) {
            settleOneDay(event.getServer());
            lastDay++;
            state.setLastSettlementDay(lastDay);
        }
    }

    private static void settleOneDay(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BankAccountHelper.applyDailyInterest(player);
        }

        PeerLoanSavedData loans = PeerLoanSavedData.get(server);
        for (PeerLoan loan : new ArrayList<>(loans.loans())) {
            loans.replaceLoan(loan.withDaysRemaining(loan.daysRemaining() - 1));
        }
        loans.setDirty();

        BondMarket.settleMaturity(server);
        FuturesMarket.settleDay(server);
        CommodityMarket.closeDay(server);
        StockMarket.closeDay(server);
    }
}
