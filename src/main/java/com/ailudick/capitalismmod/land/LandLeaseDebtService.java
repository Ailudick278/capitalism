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
        if (claim.leaseDebt() > 0L && claim.leaseeUuid() != null) {
            LandLeaseDebtSavedData.get(server).add(new LandLeaseDebtSavedData.Debt(
                    UUID.randomUUID().toString(), claim.id(), claim.leaseeUuid(), claim.ownerUuid(),
                    claim.leaseDebt(), server.overworld().getGameTime()));
        }
        return claim.clearLease();
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
