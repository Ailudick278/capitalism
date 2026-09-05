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
        long debt = claim.leaseDebt();
        LandLeaseDepositSavedData.Deposit deposit = LandLeaseDepositSavedData.get(server).take(claim.id());
        if (deposit != null && deposit.amount() > 0L) {
            long applied = Math.min(debt, deposit.amount());
            if (applied > 0L) {
                payOwner(server, claim.ownerUuid(), applied);
                debt -= applied;
            }
            long refund = deposit.amount() - applied;
            if (refund > 0L) payTenant(server, deposit.tenantUuid(), refund);
        }
        if (debt > 0L && claim.leaseeUuid() != null) {
            LandLeaseDebtSavedData.get(server).add(new LandLeaseDebtSavedData.Debt(
                    UUID.randomUUID().toString(), claim.id(), claim.leaseeUuid(), claim.ownerUuid(),
                    debt, server.overworld().getGameTime()));
        }
        return claim.clearLease();
    }

    private static void payOwner(MinecraftServer server, UUID ownerUuid, long amount) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerUuid);
        if (owner != null) EconomyHelper.giveMoney(owner, Config.defaultCurrency(), amount);
        else MarketMailboxSavedData.get(server).creditMoney(ownerUuid, Config.defaultCurrencyId(), amount);
    }

    private static void payTenant(MinecraftServer server, UUID tenantUuid, long amount) {
        ServerPlayer tenant = server.getPlayerList().getPlayer(tenantUuid);
        if (tenant != null) EconomyHelper.giveMoney(tenant, Config.defaultCurrency(), amount);
        else MarketMailboxSavedData.get(server).creditMoney(tenantUuid, Config.defaultCurrencyId(), amount);
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
            ServerPlayer owner = server.getPlayerList().getPlayer(debt.ownerUuid());
            if (owner != null) {
                EconomyHelper.giveMoney(owner, Config.defaultCurrency(), debt.amount());
            } else {
                MarketMailboxSavedData.get(server).creditMoney(debt.ownerUuid(),
                        Config.defaultCurrencyId(), debt.amount());
            }
            data.remove(debt.id());
            tenant.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "已补缴已结束租约的欠租：" + debt.amount()), true);
        }
    }
}
