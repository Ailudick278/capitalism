package com.ailudick.capitalismmod.economy.audit;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.CompanyLedgerEntry;
import com.ailudick.capitalismmod.company.CompanyLedgerSavedData;
import com.ailudick.capitalismmod.company.CompanySavedData;
import com.ailudick.capitalismmod.economy.labor.EmploymentRecord;
import com.ailudick.capitalismmod.economy.labor.LaborMarketSavedData;
import com.ailudick.capitalismmod.economy.labor.LaborPayrollSavedData;
import com.ailudick.capitalismmod.economy.FinancialSettlementJournalSavedData;
import com.ailudick.capitalismmod.economy.EconomicSettlementJournalSavedData;
import com.ailudick.capitalismmod.economy.contract.ContractDisputeSavedData;
import com.ailudick.capitalismmod.economy.contract.EconomicContractSavedData;
import com.ailudick.capitalismmod.economy.contract.ContractStatus;
import com.ailudick.capitalismmod.economy.contract.EconomicContractSavedData;
import com.ailudick.capitalismmod.population.Household;
import com.ailudick.capitalismmod.population.PopulationSavedData;
import com.ailudick.capitalismmod.population.HousingLeaseSavedData;
import com.ailudick.capitalismmod.population.PrivateLandlordSavedData;
import com.ailudick.capitalismmod.land.LandLeaseDebtSavedData;
import com.ailudick.capitalismmod.land.LandLeaseSettlementSavedData;
import com.ailudick.capitalismmod.land.LandRentBillSavedData;
import com.ailudick.capitalismmod.supply.SupplyEscrowSavedData;
import com.ailudick.capitalismmod.business.BusinessOrderEscrowSavedData;
import com.ailudick.capitalismmod.business.BusinessOrderSavedData;
import com.ailudick.capitalismmod.auction.AuctionListingIntentSavedData;
import com.ailudick.capitalismmod.market.CommodityBuyIntentSavedData;
import com.ailudick.capitalismmod.market.CommoditySellIntentSavedData;
import com.ailudick.capitalismmod.market.CommodityTradeIntentSavedData;
import com.ailudick.capitalismmod.stock.StockBuyIntentSavedData;
import com.ailudick.capitalismmod.stock.StockSellIntentSavedData;
import com.ailudick.capitalismmod.stock.StockTradeIntentSavedData;
import com.ailudick.capitalismmod.supply.SupplyOrderIntentSavedData;
import com.ailudick.capitalismmod.bank.BankCashDepositIntentSavedData;
import com.ailudick.capitalismmod.bank.BankRepaymentIntentSavedData;
import com.ailudick.capitalismmod.currency.CurrencyExchangeIntentSavedData;
import com.ailudick.capitalismmod.economy.PlayerTransferIntentSavedData;
import com.ailudick.capitalismmod.government.GovernmentPolicySavedData;
import com.ailudick.capitalismmod.bond.BondSavedData;
import com.ailudick.capitalismmod.population.HouseholdConsumptionSavedData;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Read-only invariant checks for persisted economic ledgers. */
public final class EconomyAuditService {
    private EconomyAuditService() {}

    public static List<String> audit(MinecraftServer server) {
        List<String> issues = new ArrayList<>();
        FinancialSettlementJournalSavedData financialJournal = FinancialSettlementJournalSavedData.get(server);
        financialJournal.pendingEntries().stream().limit(100)
                .forEach(entry -> issues.add("pending financial settlement "
                        + entry.transactionId() + "/" + entry.instrument() + "/" + entry.phase()));
        EconomicSettlementJournalSavedData.get(server).pendingEntries().stream().limit(100)
                .forEach(entry -> issues.add("pending daily settlement "
                        + entry.day() + "/" + entry.phase()));
        AuctionListingIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending auction listing "
                        + intent.auctionId() + "/" + intent.itemId() + "/" + intent.escrowed()));
        CommodityBuyIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending commodity buy " + intent.orderId()));
        CommoditySellIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending commodity sell " + intent.orderId()));
        CommodityTradeIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending commodity trade " + intent.id()));
        StockBuyIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending stock buy " + intent.orderId()));
        StockSellIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending stock sell " + intent.orderId()));
        StockTradeIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending stock trade " + intent.id()));
        SupplyOrderIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending supply order " + intent.orderId()));
        BankCashDepositIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending bank cash deposit " + intent.source()));
        BankRepaymentIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending bank repayment " + intent.id()));
        CurrencyExchangeIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending currency exchange " + intent.id()));
        PlayerTransferIntentSavedData.get(server).intents().stream().limit(100)
                .forEach(intent -> issues.add("pending player transfer " + intent.id()));
        GovernmentPolicySavedData government = GovernmentPolicySavedData.get(server);
        if (government.treasuryMinor() < 0L) issues.add("government treasury is negative");
        for (var transaction : government.transactions()) {
            if (transaction.id().isBlank() || transaction.amount() <= 0L || transaction.balanceAfter() < 0L) {
                issues.add("government spending transaction invalid " + transaction.id());
            }
        }
        for (var revenue : government.taxRevenues()) {
            if (revenue.id().isBlank() || revenue.originalAmount() <= 0L
                    || revenue.convertedAmount() <= 0L || revenue.balanceAfter() < 0L) {
                issues.add("government tax revenue invalid " + revenue.id());
            }
        }
        for (var holding : BondSavedData.get(server).holdings()) {
            if (holding.id().isBlank() || holding.holder() == null || holding.faceValue() <= 0L
                    || holding.ratePerYear() < 0.0 || holding.totalDays() <= 0
                    || holding.daysToMaturity() < 0 || holding.daysToMaturity() > holding.totalDays()) {
                issues.add("bond holding invalid " + holding.id());
            }
        }
        CompanySavedData companies = CompanySavedData.get(server);
        CompanyLedgerSavedData ledgers = CompanyLedgerSavedData.get(server);
        for (Company company : companies.companies().values()) {
            company.treasury().forEach((currency, balance) -> { if (balance == null || balance < 0L) issues.add("company " + company.companyId() + " negative treasury " + currency); });
            Map<String, Long> previous = new HashMap<>();
            for (CompanyLedgerEntry entry : ledgers.entries(company.companyId())) {
                if (entry.amount() == Long.MIN_VALUE || entry.balanceAfter() < 0L) issues.add("company " + company.companyId() + " invalid ledger entry at " + entry.timestamp());
                Long old = previous.get(entry.currencyId());
                if (old != null && safeAdd(old, entry.amount()) != entry.balanceAfter()) issues.add("company " + company.companyId() + " broken balance chain " + entry.currencyId() + " at " + entry.timestamp());
                previous.put(entry.currencyId(), entry.balanceAfter());
            }
        }
        PopulationSavedData population = PopulationSavedData.get(server);
        for (Household household : population.households()) if (household.cashMinor() < 0L || household.size() < household.workingAge()) issues.add("household " + household.id() + " invalid cash or age structure");
        for (var consumption : HouseholdConsumptionSavedData.get(server).records()) {
            Household household = population.find(consumption.householdId());
            long expected = safeMultiply(consumption.quantity(), consumption.unitPriceMinor());
            if (household == null) issues.add("consumption has no household " + consumption.id());
            if (consumption.id().isBlank() || consumption.itemId().isBlank() || consumption.quantity() <= 0L
                    || consumption.unitPriceMinor() <= 0L || consumption.totalCostMinor() <= 0L
                    || expected != consumption.totalCostMinor()) {
                issues.add("household consumption invalid " + consumption.id());
            }
        }
        HousingLeaseSavedData housing = HousingLeaseSavedData.get(server);
        for (var lease : housing.leases()) {
            Household household = population.find(lease.householdId());
            if (household == null) issues.add("housing lease " + lease.householdId() + " has no household");
            else if (!household.region().equals(lease.region())) {
                issues.add("housing lease " + lease.householdId() + " region differs from household");
            }
            if (lease.dailyRentMinor() < 0L || lease.arrearsMinor() < 0L || lease.depositHeldMinor() < 0L
                    || lease.depositHeldMinor() > lease.depositDueMinor() || lease.missedDays() < 0
                    || lease.missedDays() > 10000) issues.add("housing lease " + lease.householdId() + " invalid balance or status");
        }
        PrivateLandlordSavedData landlords = PrivateLandlordSavedData.get(server);
        landlords.balances().forEach((owner, balance) -> {
            try { java.util.UUID.fromString(owner); } catch (IllegalArgumentException e) { issues.add("private landlord invalid owner " + owner); }
            if (balance == null || balance < 0L) issues.add("private landlord negative receivable " + owner);
        });
        for (var receipt : landlords.receipts()) if (receipt.amount() <= 0L || receipt.balanceAfter() < 0L)
            issues.add("private landlord invalid receipt " + receipt.id());
        for (var withdrawal : landlords.withdrawals()) if (withdrawal.amount() <= 0L || withdrawal.balanceAfter() < 0L)
            issues.add("private landlord invalid withdrawal " + withdrawal.id());
        for (var settlement : LandLeaseSettlementSavedData.get(server).settlements()) {
            if (settlement.id().isBlank() || settlement.landId().isBlank() || settlement.tenantUuid() == null
                    || settlement.ownerUuid() == null || settlement.ownerAmount() < 0L
                    || settlement.tenantRefund() < 0L || settlement.debtAmount() < 0L) {
                issues.add("land lease settlement invalid " + settlement.id());
            }
        }
        for (var debt : LandLeaseDebtSavedData.get(server).debts()) {
            if (debt.id().isBlank() || debt.landId().isBlank() || debt.tenantUuid() == null
                    || debt.ownerUuid() == null || debt.amount() <= 0L) {
                issues.add("land lease debt invalid " + debt.id());
            }
        }
        for (var bill : LandRentBillSavedData.get(server).bills()) {
            if (bill.id().isBlank() || bill.landId().isBlank() || bill.tenantUuid() == null
                    || bill.ownerUuid() == null || bill.amount() <= 0L || bill.dueAt() < 0L
                    || !LandRentBillSavedData.isValidStatus(bill.status())) issues.add("land rent bill invalid " + bill.id());
        }
        for (var escrow : SupplyEscrowSavedData.get(server).escrows()) {
            long distributed = safeAdd(escrow.heldMinor(), safeAdd(escrow.releasedMinor(), escrow.refundedMinor()));
            if (escrow.originalMinor() <= 0L || escrow.heldMinor() < 0L || escrow.releasedMinor() < 0L
                    || escrow.refundedMinor() < 0L || distributed != escrow.originalMinor()) {
                issues.add("supply escrow balance mismatch " + escrow.orderId());
            }
        }
        BusinessOrderSavedData businessOrders = BusinessOrderSavedData.get(server);
        for (var escrow : BusinessOrderEscrowSavedData.get(server).escrows()) {
            long distributed = safeAdd(escrow.heldMinor(), safeAdd(escrow.releasedMinor(), escrow.refundedMinor()));
            if (escrow.orderId().isBlank() || escrow.batchId().isBlank() || escrow.buyerId().isBlank()
                    || escrow.originalMinor() <= 0L || escrow.heldMinor() < 0L
                    || escrow.releasedMinor() < 0L || escrow.refundedMinor() < 0L
                    || distributed != escrow.originalMinor()) {
                issues.add("business order escrow balance mismatch " + escrow.orderId() + "/" + escrow.batchId());
            }
            var order = businessOrders.get(escrow.orderId());
            if (order == null) {
                issues.add("business order escrow has no order " + escrow.orderId());
            } else if (population.find(escrow.buyerId()) == null) {
                issues.add("business order escrow has no buyer household " + escrow.orderId() + "/" + escrow.batchId());
            } else if ("completed".equals(order.status()) && escrow.heldMinor() != 0L) {
                issues.add("completed business order still has held escrow " + escrow.orderId() + "/" + escrow.batchId());
            } else if (("cancelled".equals(order.status()) || "expired".equals(order.status()))
                    && escrow.heldMinor() != 0L) {
                issues.add("closed business order still has held escrow " + escrow.orderId() + "/" + escrow.batchId());
            }
        }
        LaborMarketSavedData labor = LaborMarketSavedData.get(server);
        for (EmploymentRecord employment : labor.employments()) if (employment.dailyWageMinor() < 0L || employment.workerId().isBlank() || employment.employerId().isBlank()) issues.add("employment " + employment.id() + " invalid participant or wage");
        for (var entry : labor.offers()) if (entry.vacancies() < 0 || entry.dailyWageMinor() <= 0L) issues.add("job offer " + entry.id() + " invalid vacancy or wage");
        Set<String> activeWorkers = new HashSet<>();
        for (EmploymentRecord employment : labor.employments()) {
            if (!employment.active()) continue;
            if (!activeWorkers.add(employment.workerId())) {
                issues.add("worker has multiple active employments " + employment.workerId());
            }
            if (population.find(employment.workerId()) == null) {
                issues.add("employment worker has no household " + employment.id());
            }
            if (CompanySavedData.get(server).get(employment.employerId()) == null) {
                issues.add("employment employer has no company " + employment.id());
            }
        }
        for (var offer : labor.offers()) {
            if (CompanySavedData.get(server).get(offer.employerId()) == null) {
                issues.add("job offer employer has no company " + offer.id());
            }
            if (offer.region().isBlank()) issues.add("job offer has blank region " + offer.id());
        }
        for (var entry : labor.employments()) { var account = LaborPayrollSavedData.get(server).account(entry.id()); if (account.unpaid() < 0L) issues.add("payroll " + entry.id() + " negative arrears"); }
        EconomicContractSavedData contracts = EconomicContractSavedData.get(server);
        for (var contract : contracts.contracts()) {
            if (contract.agreedQuantity() > 0L && contract.fulfilledQuantity() > contract.agreedQuantity()) {
                issues.add("contract " + contract.id() + " fulfilled quantity exceeds agreement");
            }
            if (contract.status() == ContractStatus.COMPLETED && contract.agreedQuantity() > 0L
                    && contract.fulfilledQuantity() < contract.agreedQuantity()) {
                issues.add("contract " + contract.id() + " completed before full fulfillment");
            }
            if (contract.status() == ContractStatus.BREACHED && contract.breachAmountMinor() <= 0L) {
                issues.add("contract " + contract.id() + " breached without exposure amount");
            }
        }
        for (var dispute : ContractDisputeSavedData.get(server).disputes()) {
            var contract = contracts.find(dispute.contractId());
            if (contract == null) {
                issues.add("dispute has no contract " + dispute.id());
            } else if ("OPEN".equals(dispute.status()) && contract.status() != ContractStatus.DISPUTED) {
                issues.add("open dispute contract is not disputed " + dispute.id());
            } else if (!"OPEN".equals(dispute.status()) && contract.status() == ContractStatus.DISPUTED) {
                issues.add("resolved dispute contract remains disputed " + dispute.id());
            }
        }
        return List.copyOf(issues);
    }
    public static boolean isBalanceChainValid(List<CompanyLedgerEntry> entries) {
        Map<String, Long> previous = new HashMap<>();
        for (CompanyLedgerEntry entry : entries) {
            if (entry == null || entry.balanceAfter() < 0L) return false;
            Long old = previous.get(entry.currencyId());
            if (old != null && safeAdd(old, entry.amount()) != entry.balanceAfter()) return false;
            previous.put(entry.currencyId(), entry.balanceAfter());
        }
        return true;
    }
    private static long safeAdd(long a, long b) { try { return Math.addExact(a, b); } catch (ArithmeticException e) { return Long.MIN_VALUE; } }
    private static long safeMultiply(long a, long b) { try { return Math.multiplyExact(a, b); } catch (ArithmeticException e) { return Long.MIN_VALUE; } }
}
