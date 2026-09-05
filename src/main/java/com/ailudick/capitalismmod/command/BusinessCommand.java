package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.business.IndividualBusiness;
import com.ailudick.capitalismmod.business.BusinessTypes;
import com.ailudick.capitalismmod.business.BusinessLedgerEntry;
import com.ailudick.capitalismmod.business.BusinessLedgerSavedData;
import com.ailudick.capitalismmod.business.IndividualBusinessHelper;
import com.ailudick.capitalismmod.business.NationalIndustryCatalog;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.tax.IndividualTaxPeriodSavedData;
import com.ailudick.capitalismmod.tax.TaxExpenseLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxRuleService;
import com.ailudick.capitalismmod.tax.TaxType;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxIncomeVoucherLedgerSavedData;

/** Commands for the early-game sole proprietor stage. */
public final class BusinessCommand {
    private BusinessCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("business");
        root.then(Commands.literal("register")
                .then(Commands.argument("name", StringArgumentType.word())
                    .then(Commands.argument("scope", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(BusinessTypes.all().keySet(), builder))
                                .executes(ctx -> register(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"),
                                        StringArgumentType.getString(ctx, "scope"))))));
        root.then(Commands.literal("info").executes(ctx -> info(ctx.getSource())));
        root.then(Commands.literal("scopes").executes(ctx -> scopes(ctx.getSource())));
        root.then(Commands.literal("industries").executes(ctx -> industries(ctx.getSource())));
        root.then(Commands.literal("ledger").executes(ctx -> ledger(ctx.getSource())));
        root.then(Commands.literal("tax").executes(ctx -> tax(ctx.getSource())));
        root.then(Commands.literal("declare")
                .then(Commands.argument("billId", StringArgumentType.word())
                        .executes(ctx -> declareTax(ctx.getSource(),
                                StringArgumentType.getString(ctx, "billId")))));
        root.then(Commands.literal("order")
                .then(Commands.literal("create")
                        .then(Commands.argument("item", StringArgumentType.word())
                                .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("unitPrice", LongArgumentType.longArg(1))
                                                .then(Commands.argument("days", IntegerArgumentType.integer(1, 365))
                                                        .executes(ctx -> createOrder(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "item"),
                                                                IntegerArgumentType.getInteger(ctx, "quantity"),
                                                                LongArgumentType.getLong(ctx, "unitPrice"),
                                                                IntegerArgumentType.getInteger(ctx, "days"))))))))
                .then(Commands.literal("list").executes(ctx -> listOrders(ctx.getSource())))
                .then(Commands.literal("deliver")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> deliverOrder(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("cancel")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> cancelOrder(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))));
        root.then(Commands.literal("deposit")
                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                        .executes(ctx -> deposit(ctx.getSource(), LongArgumentType.getLong(ctx, "amount")))));
        root.then(Commands.literal("withdraw")
                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                        .executes(ctx -> withdraw(ctx.getSource(), LongArgumentType.getLong(ctx, "amount")))));
        root.then(Commands.literal("paytax").executes(ctx -> payTax(ctx.getSource())));
        root.then(Commands.literal("close").executes(ctx -> close(ctx.getSource())));
        dispatcher.register(root);
    }

    private static int register(CommandSourceStack source, String name, String scope) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!IndividualBusinessHelper.register(player, name, scope)) {
            source.sendFailure(Component.literal("个体户已登记，或名称无效；经营范围必须使用内置项目 ID。"));
            return 0;
        }
        IndividualBusiness business = IndividualBusinessHelper.get(player);
        source.sendSuccess(() -> Component.literal("个体户登记成功：" + business.name()
                + " | 统一社会信用代码：" + business.businessId()), false);
        return 1;
    }

    private static int info(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        IndividualBusiness business = IndividualBusinessHelper.get(player);
        if (business == null) {
            source.sendFailure(Component.literal("你尚未登记个体户。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("个体户：" + business.name()
                + " | 经营范围：" + BusinessTypes.displayName(business.scope()) + " (" + business.scope() + ")"
                + " | 状态：" + business.status()
                + " | 经营账户 USD：" + business.balance("usd")
                + " | 欠税：" + business.taxOwed()
                + " | ID：" + business.businessId()), false);
        return 1;
    }

    private static int scopes(CommandSourceStack source) {
        BusinessTypes.all().values().forEach(definition -> source.sendSuccess(() -> Component.literal(
                definition.id() + " | " + definition.displayName() + " | " + definition.category()
                        + (definition.requiresPermit() ? " | 需要许可证" : " | 一般项目")
                        + (definition.available() ? " | 当前开放" : " | 当前未开放")), false));
        return BusinessTypes.all().size();
    }

    private static int industries(CommandSourceStack source) {
        NationalIndustryCatalog.all().forEach(industry -> source.sendSuccess(() -> Component.literal(
                industry.code() + " | " + industry.name() + " | level=" + industry.level()
                        + " | parent=" + (industry.parentCode().isEmpty() ? "-" : industry.parentCode())), false));
        return NationalIndustryCatalog.all().size();
    }

    private static int ledger(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        IndividualBusiness business = IndividualBusinessHelper.get(player);
        if (business == null) {
            source.sendFailure(Component.literal("你尚未登记个体户。"));
            return 0;
        }
        for (BusinessLedgerEntry entry : BusinessLedgerSavedData.get(player.getServer()).entries(business.businessId())) {
            source.sendSuccess(() -> Component.literal(entry.type() + " | " + entry.amount()
                    + " " + entry.currencyId() + " | 余额 " + entry.balanceAfter()
                    + " | " + entry.description()), false);
        }
        return 1;
    }

    private static int tax(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        IndividualBusiness business = IndividualBusinessHelper.get(player);
        if (business == null) {
            source.sendFailure(Component.literal("你尚未登记个体户。"));
            return 0;
        }
        long now = player.getServer().overworld().getGameTime();
        var periods = IndividualTaxPeriodSavedData.get(player.getServer()).forBusiness(business.businessId());
        var current = periods.stream().filter(entry -> entry.periodStart() <= now && now < entry.periodEnd())
                .findFirst().orElse(null);
        if (current == null) {
            source.sendSuccess(() -> Component.literal("当前税务周期暂无收入或可扣除成本记录。"), false);
        } else {
            long profit = Math.max(0L, current.revenue() - Math.min(current.revenue(), current.expenses()));
            long baseMinor = Money.toMinorSaturated(profit);
            long taxMinor = TaxRuleService.taxMinor(player.getServer(), TaxType.INDIVIDUAL_BUSINESS_INCOME,
                    baseMinor, now);
            int rate = TaxRuleService.rateBasisPoints(player.getServer(), TaxType.INDIVIDUAL_BUSINESS_INCOME, now);
            source.sendSuccess(() -> Component.literal("个人经营税务申报 | 周期 " + current.periodStart()
                    + " - " + current.periodEnd()), false);
            source.sendSuccess(() -> Component.literal("收入：USD " + current.revenue()
                    + " | 可扣除采购成本：USD " + current.expenses()), false);
            source.sendSuccess(() -> Component.literal("应税所得：USD " + profit
                    + " | 税率：" + (rate / 100.0) + "% | 预计税额：" + Money.format(taxMinor)), false);
        }
        var expenses = TaxExpenseLedgerSavedData.get(player.getServer()).forTaxpayer(player.getUUID()).stream()
                .filter(expense -> expense.subjectId().equals(business.businessId()))
                .toList();
        source.sendSuccess(() -> Component.literal("已登记费用凭证：" + expenses.size() + " 条"), false);
        var incomes = TaxIncomeVoucherLedgerSavedData.get(player.getServer()).forTaxpayer(player.getUUID()).stream()
                .filter(income -> income.subjectId().equals(business.businessId()))
                .toList();
        source.sendSuccess(() -> Component.literal("已登记收入凭证：" + incomes.size() + " 条"), false);
        var bills = TaxLedgerSavedData.get(player.getServer()).allFor(player.getUUID()).stream()
                .filter(bill -> bill.subject().subjectId().equals(business.businessId())
                        && bill.subject().type() == TaxType.INDIVIDUAL_BUSINESS_INCOME)
                .toList();
        if (bills.isEmpty()) {
            source.sendSuccess(() -> Component.literal("已生成税单：暂无"), false);
        } else {
            source.sendSuccess(() -> Component.literal("已生成税单：" + bills.size() + " 张"), false);
            bills.stream().limit(5).forEach(bill -> source.sendSuccess(() -> Component.literal(
                    bill.id() + " | 应缴 " + Money.format(bill.amount()) + " | 未缴 "
                            + Money.format(bill.outstanding()) + " | " + (bill.paid() ? "已缴清" : "待缴")), false));
        }
        return 1;
    }

    private static int declareTax(CommandSourceStack source, String billId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        IndividualBusiness business = IndividualBusinessHelper.get(player);
        if (business == null) {
            source.sendFailure(Component.literal("你尚未登记个体户。"));
            return 0;
        }
        var bill = TaxLedgerSavedData.get(player.getServer()).get(billId);
        if (bill == null || bill.subject().taxpayerUuid() == null
                || !bill.subject().taxpayerUuid().equals(player.getUUID())
                || bill.subject().type() != TaxType.INDIVIDUAL_BUSINESS_INCOME
                || !bill.subject().subjectId().equals(business.businessId())) {
            source.sendFailure(Component.literal("税单不存在，或不属于你的个人经营税务。"));
            return 0;
        }
        if (bill.declared()) {
            source.sendSuccess(() -> Component.literal("该个人经营税单已经申报。"), false);
            return 1;
        }
        if (!TaxService.declare(player, bill.id())) {
            source.sendFailure(Component.literal("个人经营税单申报失败。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("个人经营税单已申报：" + bill.id()
                + "。现在可以使用 /business paytax 缴税。"), false);
        return 1;
    }

    private static int createOrder(CommandSourceStack source, String item, int quantity, long unitPrice, int days)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var order = IndividualBusinessHelper.createOrder(player, item, quantity, unitPrice, days);
        if (order == null) {
            source.sendFailure(Component.literal("个体户不存在、商品无效，或订单参数不合法。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("销售订单已发布：" + order.id()
                + " | " + order.itemId() + " x" + order.quantity() + " | 单价 USD " + order.unitPrice()), false);
        return 1;
    }

    private static int listOrders(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        IndividualBusiness business = IndividualBusinessHelper.get(player);
        if (business == null) {
            source.sendFailure(Component.literal("你尚未登记个体户。"));
            return 0;
        }
        int count = 0;
        for (var order : com.ailudick.capitalismmod.business.BusinessOrderSavedData.get(player.getServer()).orders().values()) {
            if (order.businessId().equals(business.businessId())) {
                source.sendSuccess(() -> Component.literal(order.id() + " | " + order.status() + " | "
                        + order.itemId() + " x" + order.remaining() + "/" + order.quantity()
                        + " | USD " + order.unitPrice() + " | 截止 tick " + order.deadline()), false);
                count++;
            }
        }
        if (count == 0) source.sendSuccess(() -> Component.literal("暂无订单。"), false);
        return count;
    }

    private static int deliverOrder(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!IndividualBusinessHelper.deliverOrder(player, id)) {
            source.sendFailure(Component.literal("订单不存在、已过期、货物不足或不属于你的个体户。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("订单已交付，货款已进入个体户经营账户。"), false);
        return 1;
    }

    private static int cancelOrder(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!IndividualBusinessHelper.cancelOrder(player, id)) {
            source.sendFailure(Component.literal("订单不存在、已完成或不属于你的个体户。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("订单已取消。"), false);
        return 1;
    }

    private static int deposit(CommandSourceStack source, long amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!IndividualBusinessHelper.deposit(player, amount)) {
            source.sendFailure(Component.literal("现金不足，或尚未登记个体户。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("已存入个体户经营账户 USD " + amount + "。"), false);
        return 1;
    }

    private static int withdraw(CommandSourceStack source, long amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!IndividualBusinessHelper.withdraw(player, amount)) {
            source.sendFailure(Component.literal("经营账户余额不足，或尚未登记个体户。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("业主提款 USD " + amount + "。"), false);
        return 1;
    }

    private static int payTax(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!IndividualBusinessHelper.payTax(player)) {
            source.sendFailure(Component.literal("没有可缴纳的个体户税款，或 USD 余额不足。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("个体户税款已通过统一税务系统缴清。"), false);
        return 1;
    }

    private static int close(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!IndividualBusinessHelper.close(player)) {
            source.sendFailure(Component.literal("账户余额或欠税不为零，无法注销个体户。"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("个体户已注销，登记记录仍保留。"), false);
        return 1;
    }
}
