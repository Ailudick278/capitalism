package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.bank.BankAccountHelper;
import com.ailudick.capitalismmod.auction.AuctionMarket;
import com.ailudick.capitalismmod.bond.BondMarket;
import com.ailudick.capitalismmod.economy.EconomySettlementSavedData;
import com.ailudick.capitalismmod.economy.EconomicSettlementJournalSavedData;
import com.ailudick.capitalismmod.futures.FuturesMarket;
import com.ailudick.capitalismmod.loan.PeerLoan;
import com.ailudick.capitalismmod.loan.PeerLoanSavedData;
import com.ailudick.capitalismmod.loan.PeerLoanNotificationService;
import com.ailudick.capitalismmod.loan.CompanyLoan;
import com.ailudick.capitalismmod.loan.CompanyLoanSavedData;
import com.ailudick.capitalismmod.tax.TaxRefundService;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.company.CompanyLifecycleService;
import com.ailudick.capitalismmod.company.CompanyPayrollService;
import com.ailudick.capitalismmod.economy.labor.LaborPayrollService;
import com.ailudick.capitalismmod.economy.contract.EconomicContractBridge;
import com.ailudick.capitalismmod.population.PopulationService;
import com.ailudick.capitalismmod.government.GovernmentPolicyService;
import com.ailudick.capitalismmod.government.GovernmentPublicBudgetService;
import com.ailudick.capitalismmod.risk.FinancialRiskService;
import com.ailudick.capitalismmod.risk.FinancialCrisisService;
import com.ailudick.capitalismmod.bank.BankLiquidityService;
import com.ailudick.capitalismmod.bank.BankCapitalService;
import com.ailudick.capitalismmod.market.CommodityMarket;
import com.ailudick.capitalismmod.market.LogisticsLossService;
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
            CompanyHelper.syncRegistryOwnership(player);
            PopulationService.ensurePlayerHousehold(player);
            SupplyMarket.recoverPendingOrderIntents(player.getServer());
            LogisticsLossService.recoverSupplyCompensations(player.getServer());
            FuturesMarket.recoverPendingOpenPositions(player.getServer());
            CommodityMarket.recoverPendingBuyIntents(player.getServer());
            CommodityMarket.recoverPendingSellIntents(player.getServer());
            StockMarket.recoverPendingBuyIntents(player.getServer());
            StockMarket.recoverPendingSellIntents(player.getServer());
            AuctionMarket.recoverListingIntents(player.getServer());
            BankAccountHelper.recoverCashPayouts(player);
            BankAccountHelper.recoverCashDeposits(player);
            settlePlayerToDay(player, player.getServer().overworld().getGameTime() / TICKS_PER_DAY);
            PeerLoanNotificationService.deliver(player);
        }
    }

    private static void settleOneDay(net.minecraft.server.MinecraftServer server, long settlementDay) {
        SupplyMarket.recoverPendingOrderIntents(server);
        LogisticsLossService.recoverSupplyCompensations(server);
        FuturesMarket.recoverPendingOpenPositions(server);
        EconomicSettlementJournalSavedData journal = EconomicSettlementJournalSavedData.get(server);
        journal.markStarted(settlementDay, "households-and-labor", server.overworld().getGameTime());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            settlePlayerToDay(player, settlementDay);
        }

        PeerLoanSavedData loans = PeerLoanSavedData.get(server);
        for (PeerLoan loan : new ArrayList<>(loans.loans())) {
            if (loan.lastSettlementDay() >= settlementDay) continue;
            PeerLoan updated = loan.withDaysRemaining(loan.daysRemaining() - 1)
                    .withLastSettlementDay(settlementDay);
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
        CompanyHelper.recoverDividendPayouts(server);
        PopulationService.matchResidents(server, settlementDay);
        LaborPayrollService.settleDaily(server, settlementDay);
        com.ailudick.capitalismmod.loan.PeerLoanHelper.recoverRecordedPayments(server);
        GovernmentPolicyService.settleDaily(server, settlementDay);
        GovernmentPublicBudgetService.settleDaily(server, settlementDay);
        PopulationService.settleDaily(server, settlementDay);
        journal.markCompleted(settlementDay, "households-and-labor", server.overworld().getGameTime());
        EconomicContractBridge.syncFreight(server);
        TaxRefundService.recoverUnfinished(server);

        CompanyLoanSavedData companyLoans = CompanyLoanSavedData.get(server);
        CompanySavedData companies = CompanySavedData.get(server);
        for (CompanyLoan loan : new ArrayList<>(companyLoans.loans())) {
            if (loan.lastSettlementDay() >= settlementDay) {
                continue;
            }
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
                if (nextDays < -Config.COMPANY_LOAN_LIQUIDATION_GRACE_DAYS.get()
                        && CompanyLifecycleService.forceLiquidation(server, company.companyId(),
                        "automatic liquidation after prolonged company-loan default")) {
                    SupplyMarket.removeOffersForCompany(server, company.ownerUuid(), company.name());
                    PeerLoanNotificationService.notify(server, company.ownerUuid(), "liquidation:" + company.companyId(),
                            "Company entered liquidation after prolonged loan default. Settle its assets and liabilities.");
                }
            }
            companyLoans.replace(loan.withDaysRemaining(nextDays).withLastSettlementDay(settlementDay));
        }

        journal.markStarted(settlementDay, "credit-and-securities", server.overworld().getGameTime());
        BondMarket.settleMaturity(server, settlementDay);
        BondMarket.recoverIssuances(server);
        BankCapitalService.settleDaily(server, settlementDay);
        FinancialRiskService.settleDaily(server, settlementDay);
        BankLiquidityService.settleDaily(server, settlementDay);
        // Build the current-day liquidity snapshot before evaluating crisis
        // state, so balance-sheet and run stress are not delayed by one day.
        FinancialCrisisService.update(server, settlementDay);
        journal.markCompleted(settlementDay, "credit-and-securities", server.overworld().getGameTime());
        journal.markStarted(settlementDay, "markets-and-close", server.overworld().getGameTime());
        FuturesMarket.settleDay(server, settlementDay);
        CommodityMarket.expireOrders(server, server.overworld().getGameTime());
        StockMarket.expireOrders(server, server.overworld().getGameTime());
        SupplyMarket.expireOrders(server, server.overworld().getGameTime());
        CommodityMarket.closeDay(server);
        StockMarket.closeDay(server);
        journal.markCompleted(settlementDay, "markets-and-close", server.overworld().getGameTime());
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
        BankAccountHelper.writeOffBadDebts(player, targetDay);
        BankCapitalService.reconcilePlayerTransactions(player);
        player.setData(com.ailudick.capitalismmod.init.ModAttachments.LAST_BANK_SETTLEMENT_DAY, lastDay);
    }
}
