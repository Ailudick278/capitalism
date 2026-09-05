package com.ailudick.capitalismmod.tax;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public final class TaxRefundService {
    private TaxRefundService() {}
    public static TaxRefundSavedData.Request request(ServerPlayer player, String currencyId, long amount) {
        TaxCreditSavedData credits = TaxCreditSavedData.get(player.getServer());
        TaxRefundSavedData data = TaxRefundSavedData.get(player.getServer());
        long now = player.getServer().overworld().getGameTime();
        long periodTicks = Config.TAX_REFUND_PERIOD_DAYS.get().longValue() * 24000L;
        TaxRefundRules.Decision decision = TaxRefundRules.evaluate(
                Currencies.exists(currencyId), amount,
                Currencies.exists(currencyId) ? credits.totalFor(player.getUUID(), currencyId) : 0L,
                !data.pendingFor(player.getUUID()).isEmpty(),
                data.countSince(player.getUUID(), Math.max(0L, now - periodTicks)));
        if (decision.result() == TaxRefundRules.Result.REJECTED) {
            TaxRefundSavedData.Request rejected = new TaxRefundSavedData.Request(UUID.randomUUID().toString(), player.getUUID(), currencyId,
                    Math.max(0L, amount), now, "REJECTED", now, "SYSTEM_RULES", decision.reason());
            data.add(rejected);
            TaxRefundAuditSavedData.get(player.getServer()).log(new TaxRefundAuditSavedData.Event(rejected.id(), player.getUUID(), "SUBMIT", "SYSTEM_RULES", currencyId, rejected.amount(), now, "REJECTED", decision.reason(), ""));
            TaxRefundNotificationService.notify(player.getServer(), rejected.id(), player.getUUID(), "Tax refund rejected: " + decision.reason());
            return rejected;
        }
        TaxRefundSavedData.Request request = new TaxRefundSavedData.Request(UUID.randomUUID().toString(), player.getUUID(), currencyId, amount,
                now, "PENDING", 0L, "", decision.reason(), credits.sourceSummaryFor(player.getUUID(), currencyId),
                credits.allocationSummaryFor(player.getUUID(), currencyId, amount), credits.allocationsFor(player.getUUID(), currencyId, amount));
        if (!data.add(request)) return null;
        TaxRefundAuditSavedData.get(player.getServer()).log(new TaxRefundAuditSavedData.Event(request.id(), player.getUUID(), "SUBMIT", "PLAYER", currencyId, amount, now, "PENDING", decision.reason(), request.sourceSummary()));
        // Small, fully covered claims satisfy the built-in regulations and settle automatically.
        if (decision.result() == TaxRefundRules.Result.AUTO_APPROVED) {
            approve(player.getServer(), request.id(), "SYSTEM_RULES");
            return data.get(request.id());
        }
        return request;
    }
    public static boolean approve(MinecraftServer server, String id, String reviewer) {
        TaxRefundSavedData data = TaxRefundSavedData.get(server); TaxRefundSavedData.Request r = data.get(id);
        if (r == null || !r.status().equals("PENDING")) return false;
        if (!Currencies.exists(r.currencyId())) return failReview(data, r, server, "Refund currency is no longer valid.");
        TaxCreditSavedData credits = TaxCreditSavedData.get(server);
        if (credits.totalFor(r.taxpayerUuid(), r.currencyId()) < r.amount()) {
            return failReview(data, r, server, "Available tax credit is lower than the requested refund.");
        }
        var currentAllocations = credits.allocationsFor(r.taxpayerUuid(), r.currencyId(), r.amount());
        long allocated = currentAllocations.stream().mapToLong(TaxRefundAllocation::refundAmount).sum();
        if (allocated != r.amount() || currentAllocations.stream().anyMatch(allocation -> allocation.refundAmount() <= 0L
                || allocation.originalCredit() < allocation.refundAmount() || allocation.sourceId().isBlank())) {
            return failReview(data, r, server, "Refund source allocation failed final verification.");
        }
        ServerPlayer player = server.getPlayerList().getPlayer(r.taxpayerUuid());
        if (player == null && !Config.TAX_REFUND_ALLOW_OFFLINE.get()) {
            return failReview(data, r, server, "Offline refund delivery is disabled by server rules.");
        }
        String allocations = credits.allocationSummaryFor(r.taxpayerUuid(), r.currencyId(), r.amount());
        long used = credits.consumeFor(r.taxpayerUuid(), r.currencyId(), r.amount());
        if (used != r.amount()) return failReview(data, r, server, "Refund credit changed during final verification.");
        if (player != null) EconomyHelper.giveMoney(player, Currencies.byId(r.currencyId()), r.amount());
        else MarketMailboxSavedData.get(server).creditMoney(r.taxpayerUuid(), r.currencyId(), r.amount());
        data.replace(new TaxRefundSavedData.Request(r.id(), r.taxpayerUuid(), r.currencyId(), r.amount(), r.requestedAt(), "APPROVED", server.overworld().getGameTime(), reviewer, r.reason(), r.sourceSummary(), allocations, currentAllocations));
        TaxRefundAuditSavedData.get(server).log(new TaxRefundAuditSavedData.Event(r.id(), r.taxpayerUuid(), "APPROVE", reviewer, r.currencyId(), r.amount(), server.overworld().getGameTime(), "APPROVED", r.reason(), allocations));
        TaxRefundNotificationService.notify(server, r.id(), r.taxpayerUuid(), "Tax refund approved: " + r.currencyId().toUpperCase() + " " + com.ailudick.capitalismmod.currency.Money.format(r.amount()));
        return true;
    }

    private static boolean failReview(TaxRefundSavedData data, TaxRefundSavedData.Request request,
                                      MinecraftServer server, String reason) {
        data.replace(new TaxRefundSavedData.Request(request.id(), request.taxpayerUuid(), request.currencyId(), request.amount(),
                request.requestedAt(), "REJECTED", server.overworld().getGameTime(), "SYSTEM_REVIEW", reason,
                request.sourceSummary(), request.allocations(), request.allocationDetails()));
        TaxRefundAuditSavedData.get(server).log(new TaxRefundAuditSavedData.Event(request.id(), request.taxpayerUuid(), "FINAL_REVIEW", "SYSTEM_REVIEW", request.currencyId(), request.amount(), server.overworld().getGameTime(), "REJECTED", reason, request.sourceSummary()));
        TaxRefundNotificationService.notify(server, request.id(), request.taxpayerUuid(), "Tax refund review failed: " + reason);
        return false;
    }
    public static boolean reject(MinecraftServer server, String id, String reviewer, String reason) {
        TaxRefundSavedData data = TaxRefundSavedData.get(server); TaxRefundSavedData.Request r = data.get(id);
        if (r == null || !r.status().equals("PENDING")) return false;
        data.replace(new TaxRefundSavedData.Request(r.id(), r.taxpayerUuid(), r.currencyId(), r.amount(), r.requestedAt(), "REJECTED", server.overworld().getGameTime(), reviewer, reason, r.sourceSummary(), r.allocations(), r.allocationDetails()));
        TaxRefundAuditSavedData.get(server).log(new TaxRefundAuditSavedData.Event(r.id(), r.taxpayerUuid(), "REJECT", reviewer, r.currencyId(), r.amount(), server.overworld().getGameTime(), "REJECTED", reason, r.sourceSummary()));
        TaxRefundNotificationService.notify(server, r.id(), r.taxpayerUuid(), "Tax refund rejected: " + reason);
        return true;
    }
}
