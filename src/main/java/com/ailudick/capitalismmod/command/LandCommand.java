package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.land.LandClaim;
import com.ailudick.capitalismmod.land.LandHelper;
import com.ailudick.capitalismmod.land.LandPurpose;
import com.ailudick.capitalismmod.land.LandSavedData;
import com.ailudick.capitalismmod.land.LandPermissionSavedData;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.land.LandMarketSavedData;
import com.ailudick.capitalismmod.land.LandTransferSavedData;
import com.ailudick.capitalismmod.land.LandOperationLogSavedData;
import com.ailudick.capitalismmod.land.LandAuctionSavedData;
import com.ailudick.capitalismmod.land.LandValuationHelper;
import com.ailudick.capitalismmod.land.LandStatus;
import com.ailudick.capitalismmod.land.LandOwnershipSavedData;
import com.ailudick.capitalismmod.tax.TaxTransactionService;
import com.ailudick.capitalismmod.tax.TaxType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class LandCommand {
    private LandCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("land");
        root.then(Commands.literal("claim").executes(ctx -> simple(ctx.getSource(),
                LandHelper.claim(ctx.getSource().getPlayerOrException()),
                "区块领地已申请。", "该区块已被占用。")));
        root.then(Commands.literal("release").executes(ctx -> simple(ctx.getSource(),
                LandHelper.release(ctx.getSource().getPlayerOrException()),
                "领地已释放。", "你不是该领地所有者。")));
        root.then(Commands.literal("info").executes(ctx -> info(ctx.getSource())));
        root.then(Commands.literal("ownership").executes(ctx -> ownership(ctx.getSource())));
        root.then(Commands.literal("logs")
                .executes(ctx -> logs(ctx.getSource(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> logs(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page")))));
        root.then(Commands.literal("logsat")
                .then(Commands.argument("dimension", StringArgumentType.word())
                        .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                                .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                        .executes(ctx -> logsAt(ctx.getSource(), StringArgumentType.getString(ctx, "dimension"),
                                                IntegerArgumentType.getInteger(ctx, "chunkX"),
                                                IntegerArgumentType.getInteger(ctx, "chunkZ"), 1))
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(ctx -> logsAt(ctx.getSource(), StringArgumentType.getString(ctx, "dimension"),
                                                        IntegerArgumentType.getInteger(ctx, "chunkX"),
                                                        IntegerArgumentType.getInteger(ctx, "chunkZ"),
                                                        IntegerArgumentType.getInteger(ctx, "page"))))))));
        root.then(Commands.literal("market").executes(ctx -> market(ctx.getSource())));
        root.then(Commands.literal("auctions").executes(ctx -> auctions(ctx.getSource())));
        root.then(Commands.literal("redeem").executes(ctx -> redeem(ctx.getSource())));
        root.then(Commands.literal("cancelauction")
                .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                        .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                .executes(ctx -> cancelAuction(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "chunkX"),
                                        IntegerArgumentType.getInteger(ctx, "chunkZ"))))));
        root.then(Commands.literal("auctionaudit").executes(ctx -> auctionAudit(ctx.getSource())));
        root.then(Commands.literal("bid")
                .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                        .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                .then(Commands.argument("price", LongArgumentType.longArg(1))
                                        .executes(ctx -> bid(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "chunkX"),
                                                IntegerArgumentType.getInteger(ctx, "chunkZ"),
                                                LongArgumentType.getLong(ctx, "price")))))));
        root.then(Commands.literal("paytax")
                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                        .executes(ctx -> payTax(ctx.getSource(), LongArgumentType.getLong(ctx, "amount")))));
        root.then(Commands.literal("purposes").executes(ctx -> purposes(ctx.getSource())));
        root.then(Commands.literal("purpose")
                .then(Commands.argument("value", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(LandHelper.purposes(), builder))
                        .executes(ctx -> purpose(ctx.getSource(), StringArgumentType.getString(ctx, "value")))));
        root.then(Commands.literal("bindbusiness").executes(ctx -> simple(ctx.getSource(),
                LandHelper.bindBusiness(ctx.getSource().getPlayerOrException()),
                "已绑定当前个体户经营场所。", "需要拥有领地并登记个体户。")));
        root.then(Commands.literal("trust")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> trust(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), true))));
        root.then(Commands.literal("untrust")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> trust(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), false))));
        root.then(Commands.literal("permissions")
                .then(Commands.argument("build", BoolArgumentType.bool())
                        .then(Commands.argument("interact", BoolArgumentType.bool())
                                .executes(ctx -> permissions(ctx.getSource(),
                                        BoolArgumentType.getBool(ctx, "build"),
                                        BoolArgumentType.getBool(ctx, "interact"))))));
        root.then(Commands.literal("specialpermissions")
                .then(Commands.argument("container", BoolArgumentType.bool())
                        .then(Commands.argument("redstone", BoolArgumentType.bool())
                                .executes(ctx -> specialPermissions(ctx.getSource(),
                                        BoolArgumentType.getBool(ctx, "container"),
                                        BoolArgumentType.getBool(ctx, "redstone"))))));
        root.then(Commands.literal("extract")
                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                        .executes(ctx -> extract(ctx.getSource(), LongArgumentType.getLong(ctx, "amount")))));
        root.then(Commands.literal("lease")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 3650))
                                .then(Commands.argument("rent", LongArgumentType.longArg(0))
                                        .executes(ctx -> lease(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "days"),
                                                LongArgumentType.getLong(ctx, "rent")))))));
        root.then(Commands.literal("unlease").executes(ctx -> unlease(ctx.getSource())));
        root.then(Commands.literal("transfer")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> transfer(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                        .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                                .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                        .executes(ctx -> transferAt(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "chunkX"), IntegerArgumentType.getInteger(ctx, "chunkZ")))))));
        root.then(Commands.literal("sell")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("price", LongArgumentType.longArg(0))
                                .executes(ctx -> sell(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                        LongArgumentType.getLong(ctx, "price"))))));
        root.then(Commands.literal("accepttransfer").executes(ctx -> acceptTransfer(ctx.getSource())));
        root.then(Commands.literal("rejecttransfer").executes(ctx -> rejectTransfer(ctx.getSource())));
        root.then(Commands.literal("cancelsell").executes(ctx -> cancelSell(ctx.getSource())));
        dispatcher.register(root);
    }

    private static int simple(CommandSourceStack source, boolean ok, String yes, String no) {
        if (ok) {
            source.sendSuccess(() -> Component.literal(yes), false);
        } else {
            source.sendFailure(Component.literal(no));
        }
        return ok ? 1 : 0;
    }

    private static int info(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandHelper.at(player, player.blockPosition());
        if (claim == null) {
            source.sendFailure(Component.literal("当前区块没有领地。"));
            return 0;
        }
        String business = claim.linkedBusinessId().isEmpty() ? "无" : claim.linkedBusinessId();
        source.sendSuccess(() -> Component.literal("领地 " + claim.id()
                + " | 用途：" + claim.purpose()
                + " | 资源 " + claim.resourceType() + " " + claim.resourceAmount()
                + " | 欠税 " + claim.taxOwed()
                + " | 经营主体 " + business), false);
        var auction = LandAuctionSavedData.get(player.getServer()).get(claim.id());
        LandStatus status = LandStatus.resolve(claim.taxOwed(), claim.taxDueAt(), claim.taxGraceUntil(),
                auction != null, player.level().getGameTime());
        source.sendSuccess(() -> Component.literal("状态：" + status.displayName()
                + " | 缴税到期：" + formatGameTime(claim.taxDueAt())
                + " | 宽限截止：" + formatGameTime(claim.taxGraceUntil())
                + " | 处置时间：" + formatGameTime(claim.taxGraceUntil()
                + com.ailudick.capitalismmod.Config.LAND_TAX_DISPOSAL_DAYS.get() * 24000L)), false);
        if (auction != null) {
            source.sendSuccess(() -> Component.literal("拍卖：起拍价 " + auction.startPrice()
                    + " | 当前最高价 " + auction.highestBid() + " | 结束时间 " + formatGameTime(auction.endsAt())), false);
        }
        return 1;
    }

    private static int ownership(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandHelper.at(player, player.blockPosition());
        if (claim == null || (!claim.ownerUuid().equals(player.getUUID()) && !player.hasPermissions(2))) {
            source.sendFailure(Component.literal("你无权查看当前土地产权链"));
            return 0;
        }
        var history = LandOwnershipSavedData.get(player.getServer()).history(claim.id());
        source.sendSuccess(() -> Component.literal("土地产权链：" + claim.id()), false);
        for (var event : history) {
            source.sendSuccess(() -> Component.literal(event.owner().toString().substring(0, 8)
                    + " | " + formatGameTime(event.time()) + " | " + event.reason()), false);
        }
        return history.size();
    }

    private static String formatGameTime(long ticks) {
        return PerpetualCalendar.formatMinecraftTicks(ticks);
    }

    private static int logs(CommandSourceStack source, int page) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandHelper.at(player, player.blockPosition());
        if (claim == null || (!claim.ownerUuid().equals(player.getUUID()) && !player.hasPermissions(2))) {
            source.sendFailure(Component.literal("你无权查看当前土地日志"));
            return 0;
        }
        return logsAt(source, claim.dimension(), claim.chunkX(), claim.chunkZ(), page);
    }

    private static int logsAt(CommandSourceStack source, String dimension, int chunkX, int chunkZ, int page)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandSavedData.get(player.getServer()).get(dimension + ":" + chunkX + ":" + chunkZ);
        if (!player.hasPermissions(2) && (claim == null || !claim.ownerUuid().equals(player.getUUID()))) {
            source.sendFailure(Component.literal("你无权查看这块土地的日志"));
            return 0;
        }
        int pageSize = 8;
        int offset = (page - 1) * pageSize;
        var logs = LandOperationLogSavedData.get(player.getServer()).forLand(dimension, chunkX, chunkZ, offset, pageSize);
        source.sendSuccess(() -> Component.literal("土地日志 " + dimension + " [" + chunkX + ", " + chunkZ
                + "] 第 " + page + " 页"), false);
        for (var entry : logs) {
            ServerPlayer actor = player.getServer().getPlayerList().getPlayer(entry.actor());
            String name = actor == null ? entry.actor().toString().substring(0, 8) : actor.getGameProfile().getName();
            source.sendSuccess(() -> Component.literal("时间 " + entry.time() + " | " + name + " | " + entry.action()), false);
        }
        if (logs.isEmpty()) source.sendSuccess(() -> Component.literal("没有更多日志"), false);
        return logs.size();
    }

    private static int auctions(CommandSourceStack source) {
        var entries = LandAuctionSavedData.get(source.getServer()).all();
        source.sendSuccess(() -> Component.literal("当前处置土地数量：" + entries.size()), false);
        entries.stream().limit(10).forEach(auction -> source.sendSuccess(() -> Component.literal(
                auction.dimension() + " [" + auction.chunkX() + ", " + auction.chunkZ() + "] 起拍："
                        + auction.startPrice() + " 当前：" + auction.highestBid() + " 结束时间：" + formatGameTime(auction.endsAt())), false));
        return entries.size();
    }

    private static int redeem(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandHelper.at(player, player.blockPosition());
        if (claim == null || !claim.ownerUuid().equals(player.getUUID())) {
            source.sendFailure(Component.literal("当前区块不是你的土地"));
            return 0;
        }
        var auctions = LandAuctionSavedData.get(player.getServer());
        if (auctions.get(claim.id()) == null) {
            source.sendFailure(Component.literal("当前土地不在处置列表中"));
            return 0;
        }
        long owed = claim.taxOwed();
        if (owed <= 0L || !LandHelper.payTax(player, owed)) {
            source.sendFailure(Component.literal("赎回失败：请准备足额余额缴清欠税"));
            return 0;
        }
        auctions.remove(claim.id());
        source.sendSuccess(() -> Component.literal("土地已赎回，处置状态已解除"), false);
        return 1;
    }

    private static int bid(CommandSourceStack source, int chunkX, int chunkZ, long price)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String dimension = player.level().dimension().location().toString();
        var auctions = LandAuctionSavedData.get(player.getServer());
        var auction = auctions.get(dimension + ":" + chunkX + ":" + chunkZ);
        long now = player.getServer().overworld().getGameTime();
        if (auction == null || auction.endsAt() <= now) {
            source.sendFailure(Component.literal("该土地没有正在进行的拍卖"));
            return 0;
        }
        if (auction.ownerUuid().equals(player.getUUID())) {
            source.sendFailure(Component.literal("土地原所有者不能参与自己的拍卖"));
            return 0;
        }
        long minimum = auction.highestBid() == Long.MAX_VALUE ? Long.MAX_VALUE
                : Math.max(auction.startPrice(), auction.highestBid() + 1L);
        long available = EconomyHelper.getBalance(player, Currencies.CNY);
        if (price < minimum || available < price - (auction.highestBidder() != null
                && auction.highestBidder().equals(player.getUUID()) ? auction.highestBid() : 0L)) {
            source.sendFailure(Component.literal("出价必须至少为 " + minimum + "，且人民币余额足够"));
            return 0;
        }
        if (auction.highestBidder() != null && auction.highestBidder().equals(player.getUUID())) {
            EconomyHelper.giveMoney(player, Currencies.CNY, auction.highestBid());
        }
        if (!EconomyHelper.tryPay(player, Currencies.CNY, price)) {
            if (auction.highestBidder() != null && auction.highestBidder().equals(player.getUUID())) {
                EconomyHelper.tryPay(player, Currencies.CNY, auction.highestBid());
            }
            source.sendFailure(Component.literal("托管出价资金失败"));
            return 0;
        }
        if (auction.highestBidder() != null && !auction.highestBidder().equals(player.getUUID())) {
            ServerPlayer previous = player.getServer().getPlayerList().getPlayer(auction.highestBidder());
            if (previous != null) EconomyHelper.giveMoney(previous, Currencies.CNY, auction.highestBid());
            else MarketMailboxSavedData.get(player.getServer()).creditMoney(auction.highestBidder(), Currencies.CNY.id(), auction.highestBid());
        }
        auctions.put(auction.withBid(player.getUUID(), price));
        source.sendSuccess(() -> Component.literal("出价成功：" + price + "，拍卖剩余 "
                + Math.max(0L, (auction.endsAt() - now) / 24000L) + " 天"), false);
        return 1;
    }

    private static int cancelAuction(CommandSourceStack source, int chunkX, int chunkZ)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!player.hasPermissions(2)) {
            source.sendFailure(Component.literal("只有管理员可以取消土地拍卖"));
            return 0;
        }
        String id = player.level().dimension().location() + ":" + chunkX + ":" + chunkZ;
        var auctions = LandAuctionSavedData.get(player.getServer());
        var auction = auctions.get(id);
        if (auction == null) {
            source.sendFailure(Component.literal("该区块没有土地拍卖"));
            return 0;
        }
        if (auction.highestBidder() != null && auction.highestBid() > 0L) {
            MarketMailboxSavedData.get(player.getServer()).creditMoney(auction.highestBidder(), Currencies.CNY.id(), auction.highestBid());
        }
        auctions.remove(id);
        source.sendSuccess(() -> Component.literal("土地拍卖已取消，托管资金已退回"), false);
        return 1;
    }

    private static int auctionAudit(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!player.hasPermissions(2)) {
            source.sendFailure(Component.literal("只有管理员可以执行拍卖审计"));
            return 0;
        }
        var auctions = LandAuctionSavedData.get(player.getServer());
        var claims = LandSavedData.get(player.getServer()).claims();
        int repaired = 0;
        for (var auction : auctions.all()) {
            if (claims.containsKey(auction.claimId())) continue;
            if (auction.highestBidder() != null && auction.highestBid() > 0L) {
                ServerPlayer bidder = player.getServer().getPlayerList().getPlayer(auction.highestBidder());
                if (bidder != null) EconomyHelper.giveMoney(bidder, Currencies.CNY, auction.highestBid());
                else MarketMailboxSavedData.get(player.getServer()).creditMoney(
                        auction.highestBidder(), Currencies.CNY.id(), auction.highestBid());
            }
            auctions.remove(auction.claimId());
            repaired++;
        }
        int repairedCount = repaired;
        source.sendSuccess(() -> Component.literal("拍卖审计完成，修复无效记录：" + repairedCount), false);
        return repaired;
    }

    private static int market(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var market = LandMarketSavedData.get(player.getServer());
        String dimension = player.level().dimension().location().toString();
        source.sendSuccess(() -> Component.literal("当前维度土地市场：成交 " + market.count(dimension)
                + " 笔，平均成交价 " + market.average(dimension)), false);
        return 1;
    }

    private static int payTax(CommandSourceStack source, long amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean paid = LandHelper.payTax(player, amount);
        return simple(source, paid, "土地税已缴纳：" + amount, "缴税失败：土地不存在、不是所有者、金额超过欠税或余额不足");
    }

    private static int purposes(CommandSourceStack source) {
        LandPurpose.all().forEach(p -> source.sendSuccess(() -> Component.literal(
                p.code() + " " + p.name() + "：" + p.description()), false));
        return LandPurpose.all().size();
    }

    private static int permissions(CommandSourceStack source, boolean build, boolean interact)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandHelper.at(player, player.blockPosition());
        if (claim == null || !claim.ownerUuid().equals(player.getUUID())) {
            source.sendFailure(Component.literal("你不是当前土地的所有者"));
            return 0;
        }
        var permissions = LandPermissionSavedData.get(player.getServer());
        permissions.set(claim.id(), build, interact, permissions.canContainer(claim.id()), permissions.canRedstone(claim.id()));
        source.sendSuccess(() -> Component.literal("成员权限已更新：建造=" + build + "，交互=" + interact), false);
        return 1;
    }

    private static int specialPermissions(CommandSourceStack source, boolean container, boolean redstone)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandHelper.at(player, player.blockPosition());
        if (claim == null || !claim.ownerUuid().equals(player.getUUID())) {
            source.sendFailure(Component.literal("你不是当前土地的所有者"));
            return 0;
        }
        var permissions = LandPermissionSavedData.get(player.getServer());
        permissions.set(claim.id(), permissions.canBuild(claim.id()), permissions.canInteract(claim.id()), container, redstone);
        source.sendSuccess(() -> Component.literal("成员特殊权限已更新：容器=" + container + "，红石=" + redstone), false);
        return 1;
    }

    private static int purpose(CommandSourceStack source, String value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandHelper.at(player, player.blockPosition());
        if (claim == null || !claim.ownerUuid().equals(player.getUUID()) || !LandHelper.validPurpose(value)) {
            source.sendFailure(Component.literal("领地不存在、不是所有者或用途无效。"));
            return 0;
        }
        LandSavedData.get(player.getServer()).put(claim.withPurpose(value));
        source.sendSuccess(() -> Component.literal("领地用途已设为 " + value), false);
        return 1;
    }

    private static int trust(CommandSourceStack source, ServerPlayer target, boolean add) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandHelper.at(player, player.blockPosition());
        if (claim == null || !claim.ownerUuid().equals(player.getUUID())) {
            source.sendFailure(Component.literal("你不是当前领地所有者。"));
            return 0;
        }
        LandSavedData.get(player.getServer()).put(add
                ? claim.addTrusted(target.getUUID())
                : claim.removeTrusted(target.getUUID()));
        source.sendSuccess(() -> Component.literal(add ? "已授权玩家。" : "已取消授权。"), false);
        return 1;
    }

    private static int extract(CommandSourceStack source, long amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!LandHelper.extract(player, amount)) {
            source.sendFailure(Component.literal("领地不存在、资源不足、用途不符或你不是所有者。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("已开采资源 " + amount + " 单位。"), false);
        return 1;
    }

    private static int lease(CommandSourceStack source, ServerPlayer target, int days, long rent)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandHelper.at(player, player.blockPosition());
        if (claim == null || !claim.ownerUuid().equals(player.getUUID())) {
            source.sendFailure(Component.literal("你不是当前领地所有者。"));
            return 0;
        }
        long until = player.level().getGameTime() + days * 24000L;
        LandSavedData.get(player.getServer()).put(claim.withLease(target.getUUID(), until, rent));
        source.sendSuccess(() -> Component.literal("领地已出租给 " + target.getName().getString()), false);
        return 1;
    }

    private static int unlease(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandHelper.at(player, player.blockPosition());
        if (claim == null || !claim.ownerUuid().equals(player.getUUID())) {
            source.sendFailure(Component.literal("你不是当前领地所有者。"));
            return 0;
        }
        LandSavedData.get(player.getServer()).put(claim.clearLease());
        source.sendSuccess(() -> Component.literal("租赁已结束。"), false);
        return 1;
    }

    private static int transfer(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandHelper.at(player, player.blockPosition());
        if (claim == null || !claim.ownerUuid().equals(player.getUUID()) || claim.leaseeUuid() != null
                || claim.taxOwed() > 0 || claim.leaseDebt() > 0 || LandHelper.isTaxFrozen(player, claim)
                || target.getUUID().equals(player.getUUID())) {
            source.sendFailure(Component.literal("土地不存在、存在租约/欠款，或你不是所有者")); return 0;
        }
        if (LandTransferSavedData.get(player.getServer()).findForLand(claim.dimension(), claim.chunkX(), claim.chunkZ()) != null) {
            source.sendFailure(Component.literal("当前土地已经有出售请求")); return 0;
        }
        LandTransferSavedData.get(player.getServer()).put(new LandTransferSavedData.Pending(player.getUUID(), target.getUUID(),
                claim.dimension(), claim.chunkX(), claim.chunkZ(), com.ailudick.capitalismmod.Config.LAND_TRANSFER_PRICE.get(),
                player.level().getGameTime() + 24000L * 3L));
        target.displayClientMessage(Component.literal("你收到一项土地转让请求，请使用 /land accepttransfer 接受"), false);
        source.sendSuccess(() -> Component.literal("土地转让请求已发送给 " + target.getName().getString()), false);
        return 1;
    }

    private static int transferAt(CommandSourceStack source, ServerPlayer target, int chunkX, int chunkZ)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandSavedData.get(player.getServer()).get(player.level().dimension().location() + ":" + chunkX + ":" + chunkZ);
        if (claim == null || !claim.ownerUuid().equals(player.getUUID()) || claim.leaseeUuid() != null
                || claim.taxOwed() > 0 || claim.leaseDebt() > 0 || LandHelper.isTaxFrozen(player, claim)
                || target.getUUID().equals(player.getUUID())) {
            source.sendFailure(Component.literal("所选土地不可转让")); return 0;
        }
        if (LandTransferSavedData.get(player.getServer()).findForLand(claim.dimension(), chunkX, chunkZ) != null) {
            source.sendFailure(Component.literal("当前土地已经有出售请求")); return 0;
        }
        LandTransferSavedData.get(player.getServer()).put(new LandTransferSavedData.Pending(player.getUUID(), target.getUUID(),
                claim.dimension(), chunkX, chunkZ, com.ailudick.capitalismmod.Config.LAND_TRANSFER_PRICE.get(),
                player.level().getGameTime() + 24000L * 3L));
        target.displayClientMessage(Component.literal("你收到土地转让请求，请使用 /land accepttransfer 接受"), false);
        source.sendSuccess(() -> Component.literal("土地转让请求已发送"), false);
        return 1;
    }

    private static int sell(CommandSourceStack source, ServerPlayer target, long price) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandHelper.at(player, player.blockPosition());
        if (price < 0 || claim == null || !claim.ownerUuid().equals(player.getUUID())
                || claim.leaseeUuid() != null || claim.taxOwed() > 0 || claim.leaseDebt() > 0
                || LandHelper.isTaxFrozen(player, claim)
                || target.getUUID().equals(player.getUUID())) {
            source.sendFailure(Component.literal("当前土地不可出售"));
            return 0;
        }
        if (LandTransferSavedData.get(player.getServer()).findForLand(claim.dimension(), claim.chunkX(), claim.chunkZ()) != null) {
            source.sendFailure(Component.literal("当前土地已经有出售请求")); return 0;
        }
        LandTransferSavedData.get(player.getServer()).put(new LandTransferSavedData.Pending(player.getUUID(), target.getUUID(),
                claim.dimension(), claim.chunkX(), claim.chunkZ(), price, player.level().getGameTime() + 24000L * 3L));
        target.displayClientMessage(Component.literal("收到土地出售请求，价格：" + price + "，请使用 /land accepttransfer 接受"), false);
        source.sendSuccess(() -> Component.literal("土地出售请求已发送，价格：" + price), false);
        return 1;
    }

    private static int acceptTransfer(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var transfers = LandTransferSavedData.get(player.getServer());
        var pending = transfers.get(player.getUUID());
        if (pending == null || pending.expiresAt() <= player.level().getGameTime()) {
            transfers.remove(player.getUUID()); source.sendFailure(Component.literal("没有有效的土地转让请求")); return 0;
        }
        if (!pending.dimension().equals(player.level().dimension().location().toString())) {
            source.sendFailure(Component.literal("请前往转让土地所在维度后再接受")); return 0;
        }
        String id = pending.dimension() + ":" + pending.chunkX() + ":" + pending.chunkZ();
        LandSavedData data = LandSavedData.get(player.getServer()); LandClaim claim = data.get(id);
        if (claim == null || !claim.ownerUuid().equals(pending.from()) || claim.leaseeUuid() != null
                || claim.taxOwed() > 0 || claim.leaseDebt() > 0) {
            transfers.remove(player.getUUID()); source.sendFailure(Component.literal("土地状态已变化，转让失败")); return 0;
        }
        long price = pending.price();
        if (!EconomyHelper.tryPay(player, Currencies.CNY, price)) {
            source.sendFailure(Component.literal("余额不足，无法支付土地转让费：" + price));
            return 0;
        }
        data.put(claim.withOwner(player.getUUID()));
        LandOwnershipSavedData.get(player.getServer()).record(claim.id(), player.getUUID(),
                player.level().getGameTime(), "主动转让");
        LandMarketSavedData.get(player.getServer()).record(new LandMarketSavedData.Transaction(
                player.level().getGameTime(), claim.dimension(), claim.chunkX(), claim.chunkZ(), claim.purpose(), price));
        LandPermissionSavedData.get(player.getServer()).remove(claim.id());
        transfers.remove(player.getUUID());
        LandOperationLogSavedData.get(player.getServer()).record(player.level().getGameTime(), pending.from(), "土地转让给" + player.getUUID(),
                claim.dimension(), claim.chunkX(), claim.chunkZ());
        source.sendSuccess(() -> Component.literal("土地转让成功"), false);
        ServerPlayer oldOwner = player.getServer().getPlayerList().getPlayer(pending.from());
        if (oldOwner != null) EconomyHelper.giveMoney(oldOwner, Currencies.CNY, price);
        else MarketMailboxSavedData.get(player.getServer()).creditMoney(pending.from(), Currencies.CNY.id(), price);
        TaxTransactionService.assess(player.getServer(), TaxType.LAND_TRANSFER, pending.from(), Currencies.CNY.id(),
                com.ailudick.capitalismmod.currency.Money.toMinorSaturated(price),
                "land-transfer:" + claim.id() + ":" + player.getUUID(), player.level().getGameTime());
        if (oldOwner != null) oldOwner.displayClientMessage(Component.literal("土地已转让给 " + player.getName().getString()), false);
        return 1;
    }
    private static int cancelSell(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LandClaim claim = LandHelper.at(player, player.blockPosition());
        if (claim == null || !claim.ownerUuid().equals(player.getUUID())) {
            source.sendFailure(Component.literal("你不是当前土地的所有者"));
            return 0;
        }
        boolean removed = LandTransferSavedData.get(player.getServer()).removeForLand(player.getUUID(),
                claim.dimension(), claim.chunkX(), claim.chunkZ());
        return simple(source, removed, "出售请求已取消", "当前土地没有出售请求");
    }

    private static int rejectTransfer(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var transfers = LandTransferSavedData.get(player.getServer());
        var pending = transfers.get(player.getUUID());
        if (pending == null) return simple(source, false, "", "没有有效的土地转让请求");
        transfers.remove(player.getUUID());
        LandOperationLogSavedData.get(player.getServer()).record(player.level().getGameTime(), player.getUUID(),
                "拒绝土地转让", pending.dimension(), pending.chunkX(), pending.chunkZ());
        ServerPlayer seller = player.getServer().getPlayerList().getPlayer(pending.from());
        if (seller != null) seller.displayClientMessage(Component.literal("土地转让请求已被拒绝"), false);
        return simple(source, true, "已拒绝土地转让", "");
    }
}
