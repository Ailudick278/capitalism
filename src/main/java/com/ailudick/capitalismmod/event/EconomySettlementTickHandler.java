package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.bank.BankAccountHelper;
import com.ailudick.capitalismmod.bond.BondMarket;
import com.ailudick.capitalismmod.economy.EconomySettlementSavedData;
import com.ailudick.capitalismmod.futures.FuturesMarket;
import com.ailudick.capitalismmod.loan.PeerLoan;
import com.ailudick.capitalismmod.loan.PeerLoanSavedData;
import com.ailudick.capitalismmod.loan.PeerLoanNotificationService;
import com.ailudick.capitalismmod.loan.CompanyLoan;
import com.ailudick.capitalismmod.loan.CompanyLoanSavedData;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.company.CompanyLifecycleService;
import com.ailudick.capitalismmod.company.CompanyPayrollService;
import com.ailudick.capitalismmod.market.CommodityMarket;
import com.ailudick.capitalismmod.supply.SupplyMarket;
import com.ailudick.capitalismmod.stock.StockMarket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;

/** Runs every missed Minecraft-day settlement exactly once, including after restart. */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class EconomySettlementTickHandler {
    private static final long TICKS_PER_DAY = PerpetualCalendar.TICKS_PER_DAY;

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
            settleOneDay(event.getServer(), lastDay + 1);
            lastDay++;
            state.setLastSettlementDay(lastDay);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            settlePlayerToDay(player, player.getServer().overworld().getGameTime() / TICKS_PER_DAY);
            PeerLoanNotificationService.deliver(player);
        }
    }

    private static void settleOneDay(net.minecraft.server.MinecraftServer server, long settlementDay) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            settlePlayerToDay(player, settlementDay);
        }

        PeerLoanSavedData loans = PeerLoanSavedData.get(server);
        for (PeerLoan loan : new ArrayList<>(loans.loans())) {
            PeerLoan updated = loan.withDaysRemaining(loan.daysRemaining() - 1);
            if (loan.daysRemaining() > 0 && updated.daysRemaining() <= 0) {
                PeerLoanNotificationService.notify(server, loan.borrower(), "due:" + loan.id(),
                        "贷款已到期，请及时偿还本金及利息（" + loan.id().substring(0, Math.min(8, loan.id().length())) + "）");
                PeerLoanNotificationService.notify(server, loan.lender(), "due-lender:" + loan.id(),
                        "你出借的贷款已到期，等待借款人偿还（" + loan.id().substring(0, Math.min(8, loan.id().length())) + "）");
            } else if (loan.daysRemaining() >= 0 && updated.daysRemaining() < 0) {
                PeerLoanNotificationService.notify(server, loan.borrower(), "overdue:" + loan.id(),
                        "贷款已逾期，逾期利息将按规则计算（" + loan.id().substring(0, Math.min(8, loan.id().length())) + "）");
                PeerLoanNotificationService.notify(server, loan.lender(), "overdue-lender:" + loan.id(),
                        "你出借的贷款已逾期（" + loan.id().substring(0, Math.min(8, loan.id().length())) + "）");
            }
            loans.replaceLoan(updated);
        }
        loans.setDirty();

        CompanyPayrollService.settleDaily(server, settlementDay);

        CompanyLoanSavedData companyLoans = CompanyLoanSavedData.get(server);
        CompanySavedData companies = CompanySavedData.get(server);
        for (CompanyLoan loan : new ArrayList<>(companyLoans.loans())) {
            int nextDays = loan.daysRemaining() - 1;
            Company company = companies.get(loan.companyId());
            if (company != null && !Company.UNASSIGNED_OWNER.equals(company.ownerUuid())) {
                String shortId = loan.id().substring(0, Math.min(8, loan.id().length()));
                if (loan.becomesDueAfter(nextDays)) {
                    PeerLoanNotificationService.notify(server, company.ownerUuid(), "company-due:" + loan.id(),
                            "Company loan " + shortId + " is due. Repay principal and interest.");
                } else if (loan.becomesOverdueAfter(nextDays)) {
                    CompanyLifecycleService.forceSuspend(server, company.companyId(), "company loan overdue");
                    PeerLoanNotificationService.notify(server, company.ownerUuid(), "company-overdue:" + loan.id(),
                            "Company loan " + shortId + " is overdue. Penalty interest is now applied.");
                }
            }
            companyLoans.replace(loan.withDaysRemaining(nextDays));
        }

        BondMarket.settleMaturity(server);
        FuturesMarket.settleDay(server);
        CommodityMarket.expireOrders(server, server.overworld().getGameTime());
        StockMarket.expireOrders(server, server.overworld().getGameTime());
        SupplyMarket.expireOrders(server, server.overworld().getGameTime());
        CommodityMarket.closeDay(server);
        StockMarket.closeDay(server);
    }

    private static void settlePlayerToDay(ServerPlayer player, long targetDay) {
        long lastDay = player.getData(com.ailudick.capitalismmod.init.ModAttachments.LAST_BANK_SETTLEMENT_DAY);
        if (lastDay < 0) {
            lastDay = targetDay;
        }
        while (lastDay < targetDay) {
            BankAccountHelper.applyDailyInterest(player, lastDay + 1);
            lastDay++;
        }
        player.setData(com.ailudick.capitalismmod.init.ModAttachments.LAST_BANK_SETTLEMENT_DAY, lastDay);
    }
}
