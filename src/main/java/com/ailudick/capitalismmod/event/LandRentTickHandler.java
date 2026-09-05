package com.ailudick.capitalismmod.event;

import com.ailudick.capitalismmod.CapitalismMod;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.land.LandClaim;
import com.ailudick.capitalismmod.land.LandSavedData;
import com.ailudick.capitalismmod.land.LandOperationLogSavedData;
import com.ailudick.capitalismmod.land.LandPermissionSavedData;
import com.ailudick.capitalismmod.land.LandTransferSavedData;
import com.ailudick.capitalismmod.land.LandAuctionSavedData;
import com.ailudick.capitalismmod.land.LandMarketSavedData;
import com.ailudick.capitalismmod.land.LandStatus;
import com.ailudick.capitalismmod.land.LandStatusSavedData;
import com.ailudick.capitalismmod.land.LandOwnershipSavedData;
import com.ailudick.capitalismmod.land.LandValuationHelper;
import com.ailudick.capitalismmod.land.LandTaxPeriodSavedData;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import com.ailudick.capitalismmod.tax.TaxPeriod;
import com.ailudick.capitalismmod.tax.TaxRuleService;
import com.ailudick.capitalismmod.tax.TaxTransactionService;
import com.ailudick.capitalismmod.menu.LandMenu;
import com.ailudick.capitalismmod.network.ServerPayloadHandler;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Settles one rent installment per Minecraft day for active online leases. */
@EventBusSubscriber(modid = CapitalismMod.MODID)
public final class LandRentTickHandler {
    private static final long TICKS_PER_DAY = 24000L;
    private static final long GRACE_DAYS = 3L;
    private static final long TAX_TICKS_PER_DAY = 24000L;
    private LandRentTickHandler() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        LandSavedData data = LandSavedData.get(server);
        if (now % 1200L == 0L) {
            var transfers = LandTransferSavedData.get(server);
            for (var transfer : transfers.all()) {
                if (transfer.expiresAt() <= now) transfers.remove(transfer.to());
            }
        }
        if (now > 0L && now % TICKS_PER_DAY == 0L
                && TaxRuleService.rate(server, TaxType.LAND, now) > 0.0) {
            for (LandClaim claim : data.claims().values()) {
            long valuation = LandValuationHelper.suggestedPrice(server.overworld(), claim);
            long annualTaxMinor = TaxRuleService.taxMinor(server, TaxType.LAND,
                    Money.toMinorSaturated(valuation), now);
            long annualTax = Money.toMajorCeiling(annualTaxMinor);
                long dailyTax = Math.max(1L, annualTax / 365L);
                long dueAt = claim.taxDueAt() > 0L ? claim.taxDueAt()
                        : now + Config.LAND_TAX_PERIOD_DAYS.get() * TAX_TICKS_PER_DAY;
                long graceUntil = claim.taxGraceUntil() > 0L ? claim.taxGraceUntil()
                        : dueAt + Config.LAND_TAX_GRACE_DAYS.get() * TAX_TICKS_PER_DAY;
                long periodStart = Math.max(0L, dueAt - Config.LAND_TAX_PERIOD_DAYS.get() * TAX_TICKS_PER_DAY);
                LandTaxPeriodSavedData periods = LandTaxPeriodSavedData.get(server);
                periods.add(claim.id(), dailyTax, periodStart, dueAt);
                TaxSubject subject = new TaxSubject(TaxType.LAND, claim.id(), claim.ownerUuid());
                int rateBps = TaxRuleService.rateBasisPoints(server, TaxType.LAND, now);

                // Import legacy land debt once, but do not generate the current
                // period's bill until the period actually ends.
                long legacyDebtMinor = Money.toMinorSaturated(claim.taxOwed());
                if (legacyDebtMinor > TaxService.outstanding(server, subject)) {
                    TaxService.ensureOutstanding(server, subject, Config.defaultCurrencyId(), legacyDebtMinor, now, dueAt, graceUntil,
                            "land-legacy:" + claim.id(), 0L, 0L, 0L, 0);
                }
                LandTaxPeriodSavedData.Accrual accrual = periods.get(claim.id());
                if (now >= dueAt && accrual.amount() > 0L) {
                    TaxPeriod taxPeriod = new TaxPeriod(accrual.periodStart(), accrual.periodEnd(),
                            now, now + Config.LAND_TAX_GRACE_DAYS.get() * TAX_TICKS_PER_DAY);
                    TaxService.createPeriodicBill(server, subject, Config.defaultCurrencyId(), Money.toMinorSaturated(accrual.amount()),
                            taxPeriod, Money.toMinorSaturated(valuation), rateBps,
                            "land-period:" + claim.id() + ":" + accrual.periodEnd());
                    periods.clear(claim.id());
                    dueAt = now + Config.LAND_TAX_PERIOD_DAYS.get() * TAX_TICKS_PER_DAY;
                    graceUntil = dueAt + Config.LAND_TAX_GRACE_DAYS.get() * TAX_TICKS_PER_DAY;
                }
                data.put(claim.withTaxSchedule(Money.toMajorCeiling(TaxService.outstanding(server, subject)), dueAt, graceUntil));
            }
        }
        if (now % 1200L == 0L) {
            var auctions = LandAuctionSavedData.get(server);
            for (var auction : auctions.all()) {
                if (auction.endsAt() > now) continue;
                LandClaim claim = data.get(auction.claimId());
                if (claim == null) {
                    refundBid(server, auction.highestBidder(), auction.highestBid());
                    auctions.remove(auction.claimId());
                    continue;
                }
                if (auction.highestBidder() == null || auction.highestBid() <= 0L) {
                    auctions.remove(auction.claimId());
                    notifyPlayer(server, auction.ownerUuid(), "土地拍卖流拍，土地仍归你所有；请缴清欠税解除冻结");
                    continue;
                }
                long taxPaid = Math.min(claim.taxOwed(), auction.highestBid());
                long ownerPayout = auction.highestBid() - taxPaid;
                LandClaim transferred = claim.withTaxSchedule(0L, 0L, 0L).withOwner(auction.highestBidder());
                data.put(transferred);
                LandOwnershipSavedData.get(server).record(claim.id(), auction.highestBidder(), now, "拍卖成交");
                LandPermissionSavedData.get(server).remove(claim.id());
                if (ownerPayout > 0L) {
                    payOwner(server, server.getPlayerList().getPlayer(auction.ownerUuid()), auction.ownerUuid(), ownerPayout);
                }
                TaxTransactionService.assess(server, TaxType.LAND_TRANSFER, claim.ownerUuid(), Currencies.CNY.id(),
                        Money.toMinorSaturated(auction.highestBid()),
                        "land-auction:" + claim.id() + ":" + auction.endsAt(), now);
                LandMarketSavedData.get(server).record(new LandMarketSavedData.Transaction(
                        now, claim.dimension(), claim.chunkX(), claim.chunkZ(), claim.purpose(), auction.highestBid()));
                logLand(server, claim, "拍卖结算：" + auction.highestBidder() + " / " + auction.highestBid());
                notifyPlayer(server, auction.highestBidder(), "土地拍卖成功，你已获得土地；成交价：" + auction.highestBid());
                notifyPlayer(server, auction.ownerUuid(), "土地拍卖已结算，已偿还欠税，剩余款项已发放");
                auctions.remove(auction.claimId());
            }
            for (LandClaim claim : data.claims().values()) {
                long disposalAt = claim.taxGraceUntil() + Config.LAND_TAX_DISPOSAL_DAYS.get() * TICKS_PER_DAY;
                if (claim.taxOwed() > 0L && claim.taxGraceUntil() > 0L && now >= disposalAt
                        && auctions.get(claim.id()) == null) {
                    long startPrice = Math.max(0L, Math.round(LandValuationHelper.suggestedPrice(server.overworld(), claim)
                            * Config.LAND_AUCTION_START_RATE.get()));
                    auctions.put(new LandAuctionSavedData.Auction(claim.id(), claim.ownerUuid(), claim.dimension(),
                            claim.chunkX(), claim.chunkZ(), now, claim.taxOwed(), startPrice, 0L, null,
                            now + Config.LAND_AUCTION_DURATION_DAYS.get() * TICKS_PER_DAY));
                    notifyPlayer(server, claim.ownerUuid(), "土地已进入逾期处置列表，可补缴欠税后使用 /land redeem 赎回");
                }
            }
        }
        if (now % 1200L == 0L) {
            LandPermissionSavedData.get(server).removeOrphans(data.claims());
            announceStatusChanges(server, data, now);
        }
        for (LandClaim claim : data.claims().values()) {
            if (claim.leaseeUuid() == null || claim.leaseUntil() <= 0L) continue;
            if (claim.leaseUntil() <= now) {
                data.put(claim.clearLease());
                notifyPlayer(server, claim.leaseeUuid(), "土地租约已到期");
                notifyPlayer(server, claim.ownerUuid(), "你的土地租约已到期");
                continue;
            }
            long dueOffset = Math.floorMod(claim.leaseUntil(), TICKS_PER_DAY);
            if (Math.floorMod(now, TICKS_PER_DAY) != dueOffset) continue;

            ServerPlayer tenant = server.getPlayerList().getPlayer(claim.leaseeUuid());
            ServerPlayer owner = server.getPlayerList().getPlayer(claim.ownerUuid());
            if (tenant == null) {
                long debt = addSaturated(claim.leaseDebt(), claim.leaseRent());
                long graceUntil = claim.leaseGraceUntil() > 0L ? claim.leaseGraceUntil() : now + GRACE_DAYS * TICKS_PER_DAY;
                if (now >= graceUntil) {
                    data.put(claim.clearLease());
                    logLand(server, claim, "租约自动解除");
                    if (owner != null) {
                        owner.displayClientMessage(net.minecraft.network.chat.Component.literal("租客离线且欠租超过宽限期，租约已解除"), true);
                    }
                } else {
                    data.put(claim.withLeaseState(claim.leaseeUuid(), claim.leaseUntil(), claim.leaseRent(), debt, graceUntil));
                }
                continue;
            }
            long totalDue = claim.leaseRent() > Long.MAX_VALUE - claim.leaseDebt()
                    ? Long.MAX_VALUE : claim.leaseRent() + claim.leaseDebt();
            boolean paid = EconomyHelper.tryPay(tenant, Config.defaultCurrency(), totalDue);
            if (paid) {
                payOwner(server, owner, claim.ownerUuid(), totalDue);
                data.put(claim.withLeaseState(claim.leaseeUuid(), claim.leaseUntil(), claim.leaseRent(),
                        0L, 0L));
                logLand(server, claim, "租金结算:" + totalDue);
                tenant.displayClientMessage(net.minecraft.network.chat.Component.literal("已支付土地租金：" + totalDue), true);
                owner.displayClientMessage(net.minecraft.network.chat.Component.literal("已收到土地租金：" + totalDue), true);
            } else {
                long debt = addSaturated(claim.leaseDebt(), claim.leaseRent());
                long graceUntil = claim.leaseGraceUntil() > 0L ? claim.leaseGraceUntil() : now + GRACE_DAYS * TICKS_PER_DAY;
                if (now >= graceUntil) {
                    data.put(claim.clearLease());
                    logLand(server, claim, "租约自动解除");
                    tenant.displayClientMessage(net.minecraft.network.chat.Component.literal("土地欠租超过宽限期，租约已解除"), true);
                    owner.displayClientMessage(net.minecraft.network.chat.Component.literal("承租人欠租超过宽限期，租约已解除"), true);
                } else {
                    data.put(claim.withLeaseState(claim.leaseeUuid(), claim.leaseUntil(), claim.leaseRent(),
                            debt, graceUntil));
                    logLand(server, claim, "产生欠租:" + debt);
                    tenant.displayClientMessage(net.minecraft.network.chat.Component.literal("土地租金余额不足，当前欠租：" + debt), true);
                    owner.displayClientMessage(net.minecraft.network.chat.Component.literal("承租人未支付租金，当前欠租：" + debt), true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer tenant)) return;
        MinecraftServer server = tenant.getServer();
        MarketMailboxSavedData.get(server).redeem(tenant);
        LandSavedData data = LandSavedData.get(server);
        long now = server.overworld().getGameTime();
        for (LandClaim claim : data.claims().values()) {
            if (claim.leaseeUuid() == null || !claim.leaseeUuid().equals(tenant.getUUID()) || claim.leaseDebt() <= 0L) continue;
            if (claim.leaseUntil() <= now) continue;
            long debt = claim.leaseDebt();
            if (!EconomyHelper.tryPay(tenant, Config.defaultCurrency(), debt)) {
                tenant.displayClientMessage(net.minecraft.network.chat.Component.literal("租约待缴租金：" + debt), true);
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(claim.ownerUuid());
            payOwner(server, owner, claim.ownerUuid(), debt);
            data.put(claim.withLeaseState(claim.leaseeUuid(), claim.leaseUntil(), claim.leaseRent(), 0L, 0L));
            logLand(server, claim, "补缴租金:" + debt);
            tenant.displayClientMessage(net.minecraft.network.chat.Component.literal("已自动补缴土地租金：" + debt), true);
            if (owner != null) owner.displayClientMessage(net.minecraft.network.chat.Component.literal("已收到土地补缴租金：" + debt), true);
        }
    }

    private static void payOwner(MinecraftServer server, ServerPlayer owner, java.util.UUID ownerUuid, long amount) {
        if (owner != null) {
            EconomyHelper.giveMoney(owner, Config.defaultCurrency(), amount);
        } else {
            MarketMailboxSavedData.get(server).creditMoney(ownerUuid, Config.defaultCurrencyId(), amount);
        }
    }

    private static void refundBid(MinecraftServer server, java.util.UUID bidderUuid, long amount) {
        if (bidderUuid == null || amount <= 0L) return;
        ServerPlayer bidder = server.getPlayerList().getPlayer(bidderUuid);
        if (bidder != null) EconomyHelper.giveMoney(bidder, Config.defaultCurrency(), amount);
        else MarketMailboxSavedData.get(server).creditMoney(bidderUuid, Config.defaultCurrencyId(), amount);
    }

    private static long addSaturated(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static void logLand(MinecraftServer server, LandClaim claim, String action) {
        LandOperationLogSavedData.get(server).record(server.overworld().getGameTime(), claim.ownerUuid(), action,
                claim.dimension(), claim.chunkX(), claim.chunkZ());
    }

    private static void announceStatusChanges(MinecraftServer server, LandSavedData data, long now) {
        var statuses = LandStatusSavedData.get(server);
        var auctions = LandAuctionSavedData.get(server);
        for (LandClaim claim : data.claims().values()) {
            LandStatus current = LandStatus.resolve(claim.taxOwed(), claim.taxDueAt(), claim.taxGraceUntil(),
                    auctions.get(claim.id()) != null, now);
            String previousName = statuses.get(claim.id());
            if (previousName == null) {
                statuses.put(claim.id(), current);
                continue;
            }
            if (previousName.equals(current.name())) continue;
            statuses.put(claim.id(), current);
            String message = "土地状态变更：" + current.displayName();
            logLand(server, claim, message);
            notifyPlayer(server, claim.ownerUuid(), message + "（区块 " + claim.chunkX() + ", " + claim.chunkZ() + "）");
            refreshOpenLandScreens(server, claim);
        }
    }

    private static void refreshOpenLandScreens(MinecraftServer server, LandClaim claim) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.containerMenu instanceof LandMenu menu)) continue;
            if (!claim.dimension().equals(menu.dimension)) continue;
            int chunkX = menu.hasSelectedChunk ? menu.selectedChunkX : menu.chunkX;
            int chunkZ = menu.hasSelectedChunk ? menu.selectedChunkZ : menu.chunkZ;
            if (claim.chunkX() == chunkX && claim.chunkZ() == chunkZ) {
                ServerPayloadHandler.sendLandData(player, chunkX, chunkZ);
            }
        }
    }

    private static void notifyPlayer(MinecraftServer server, java.util.UUID uuid, String message) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
    }
}
