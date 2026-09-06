package com.ailudick.capitalismmod.land;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Keeps unpaid rent collectible after the underlying lease is terminated. */
public final class LandLeaseDebtService {
    private LandLeaseDebtService() {}

    public static LandClaim endLease(MinecraftServer server, LandClaim claim) {
        if (server == null || claim == null || claim.leaseeUuid() == null) return claim;
        String settlementId = settlementId(claim);
        LandLeaseSettlementSavedData settlements = LandLeaseSettlementSavedData.get(server);
        LandLeaseSettlementSavedData.Settlement settlement = settlements.find(settlementId);
        if (settlement == null) {
            LandLeaseDepositSavedData.Deposit deposit = LandLeaseDepositSavedData.get(server).find(claim.id());
            long depositAmount = deposit == null ? 0L : deposit.amount();
            long applied = Math.min(Math.max(0L, claim.leaseDebt()), depositAmount);
            settlement = new LandLeaseSettlementSavedData.Settlement(settlementId, claim.id(),
                    claim.leaseeUuid(), claim.ownerUuid(), applied, depositAmount - applied,
                    Math.max(0L, claim.leaseDebt() - applied), server.overworld().getGameTime(), false);
            if (!settlements.put(settlement)) return claim;
        }
        LandLeaseDepositSavedData.get(server).take(claim.id());
        complete(server, settlement);
        return claim.clearLease();
    }

    public static void recover(MinecraftServer server) {
        for (LandLeaseSettlementSavedData.Settlement settlement
                : LandLeaseSettlementSavedData.get(server).pending()) {
            LandLeaseDepositSavedData.get(server).take(settlement.landId());
            complete(server, settlement);
        }
    }

    private static void complete(MinecraftServer server, LandLeaseSettlementSavedData.Settlement settlement) {
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
        if (settlement.ownerAmount() > 0L) {
            String source = settlement.id() + ":owner";
            mailbox.creditMoneyOnce(settlement.ownerUuid(), Config.defaultCurrencyId(), settlement.ownerAmount(), source);
            ServerPlayer owner = server.getPlayerList().getPlayer(settlement.ownerUuid());
            if (owner != null) mailbox.redeemMoneyOnly(owner);
        }
        if (settlement.tenantRefund() > 0L) {
            String source = settlement.id() + ":tenant";
            mailbox.creditMoneyOnce(settlement.tenantUuid(), Config.defaultCurrencyId(), settlement.tenantRefund(), source);
            ServerPlayer tenant = server.getPlayerList().getPlayer(settlement.tenantUuid());
            if (tenant != null) mailbox.redeemMoneyOnly(tenant);
        }
        if (settlement.debtAmount() > 0L) {
            LandLeaseDebtSavedData.get(server).addOnce(new LandLeaseDebtSavedData.Debt(
                    settlement.id() + ":debt", settlement.landId(), settlement.tenantUuid(), settlement.ownerUuid(),
                    settlement.debtAmount(), settlement.createdAt()));
        }
        LandLeaseSettlementSavedData.get(server).complete(settlement.id());
    }

    private static String settlementId(LandClaim claim) {
        return "land-lease-end:" + claim.id() + ":" + claim.leaseeUuid()
                + ":" + claim.leaseUntil() + ":" + claim.leaseRent();
    }

    public static void settleFor(ServerPlayer tenant) {
        MinecraftServer server = tenant.getServer();
        LandLeaseDebtSavedData data = LandLeaseDebtSavedData.get(server);
        for (LandLeaseDebtSavedData.Debt debt : data.forTenant(tenant.getUUID())) {
            if (!EconomyHelper.tryPay(tenant, Config.defaultCurrency(), debt.amount())) {
                tenant.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "已结束租约的待缴租金：" + debt.amount()), true);
                continue;
            }
            MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
            mailbox.creditMoneyOnce(debt.ownerUuid(), Config.defaultCurrencyId(), debt.amount(),
                    "land-rent-debt-payment:" + debt.id());
            ServerPlayer owner = server.getPlayerList().getPlayer(debt.ownerUuid());
            if (owner != null) mailbox.redeemMoneyOnly(owner);
            data.remove(debt.id());
            tenant.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "已补缴已结束租约的欠租：" + debt.amount()), true);
        }
    }
}
