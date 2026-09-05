package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.business.IndividualBusiness;
import com.ailudick.capitalismmod.business.IndividualBusinessSavedData;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.tax.IndividualTaxPeriodSavedData;
import com.ailudick.capitalismmod.tax.TaxBill;
import com.ailudick.capitalismmod.tax.TaxLedgerSavedData;
import com.ailudick.capitalismmod.tax.TaxPeriod;
import com.ailudick.capitalismmod.tax.TaxRuleService;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import com.ailudick.capitalismmod.tax.TaxCorrectionAuditSavedData;
import com.ailudick.capitalismmod.tax.TaxCorrectionRequestSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Administrator corrections for locked individual-business tax periods. */
public final class TaxCorrectionCommand {
    private TaxCorrectionCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var request = Commands.literal("request")
                .then(Commands.argument("businessId", StringArgumentType.word())
                .then(Commands.argument("periodEnd", LongArgumentType.longArg(1))
                .then(Commands.argument("revenue", LongArgumentType.longArg(0))
                .then(Commands.argument("expenses", LongArgumentType.longArg(0))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                .executes(ctx -> submit(ctx.getSource(), StringArgumentType.getString(ctx, "businessId"),
                        LongArgumentType.getLong(ctx, "periodEnd"), LongArgumentType.getLong(ctx, "revenue"),
                        LongArgumentType.getLong(ctx, "expenses"), StringArgumentType.getString(ctx, "reason"))))))));
        var review = Commands.literal("review").requires(source -> source.hasPermission(2))
                .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.literal("approve").then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> reviewRequest(ctx.getSource(), StringArgumentType.getString(ctx, "id"), true, StringArgumentType.getString(ctx, "reason")))))
                .then(Commands.literal("reject").then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> reviewRequest(ctx.getSource(), StringArgumentType.getString(ctx, "id"), false, StringArgumentType.getString(ctx, "reason"))))));
        var correction = Commands.literal("individual")
                .requires(source -> source.hasPermission(4))
                .then(Commands.argument("businessId", StringArgumentType.word())
                .then(Commands.argument("periodEnd", LongArgumentType.longArg(1))
                .then(Commands.argument("revenue", LongArgumentType.longArg(0))
                .then(Commands.argument("expenses", LongArgumentType.longArg(0))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                .executes(ctx -> correct(ctx.getSource(), StringArgumentType.getString(ctx, "businessId"),
                        LongArgumentType.getLong(ctx, "periodEnd"), LongArgumentType.getLong(ctx, "revenue"),
                        LongArgumentType.getLong(ctx, "expenses"), StringArgumentType.getString(ctx, "reason"))))))));
        dispatcher.register(Commands.literal("taxcorrection")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("history").executes(ctx -> history(ctx.getSource())))
                .then(review)
                .then(correction));
        dispatcher.register(Commands.literal("taxcorrection_request").then(request));
    }

    private static int submit(CommandSourceStack source, String businessId, long periodEnd, long revenue,
                              long expenses, String reason) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        IndividualBusiness business = IndividualBusinessSavedData.get(source.getServer()).findByBusinessId(businessId);
        if (business == null || !business.ownerUuid().equals(player.getUUID())
                || IndividualTaxPeriodSavedData.get(source.getServer()).forBusiness(businessId).stream().noneMatch(e -> e.periodEnd() == periodEnd)) {
            source.sendFailure(Component.literal("只能为自己的已存在税务周期提交更正申请。")); return 0;
        }
        String id = java.util.UUID.randomUUID().toString();
        TaxCorrectionRequestSavedData.get(source.getServer()).add(new TaxCorrectionRequestSavedData.Request(
                id, businessId, periodEnd, revenue, expenses, reason, player.getUUID(),
                source.getServer().overworld().getGameTime(), "PENDING", "", 0L, ""));
        source.sendSuccess(() -> Component.literal("更正申请已提交：" + id), false);
        return 1;
    }

    public static int reviewRequest(CommandSourceStack source, String id, boolean approve, String reason) {
        var data = TaxCorrectionRequestSavedData.get(source.getServer());
        var request = data.get(id);
        if (request == null || !"PENDING".equals(request.status())) { source.sendFailure(Component.literal("申请不存在或已处理。")); return 0; }
        if (approve) correct(source, request.businessId(), request.periodEnd(), request.revenue(), request.expenses(), request.reason());
        data.update(new TaxCorrectionRequestSavedData.Request(request.id(), request.businessId(), request.periodEnd(), request.revenue(), request.expenses(), request.reason(), request.applicant(), request.createdAt(), approve ? "APPROVED" : "REJECTED", source.getTextName(), source.getServer().overworld().getGameTime(), reason));
        source.sendSuccess(() -> Component.literal(approve ? "更正申请已批准。" : "更正申请已驳回。"), true);
        return 1;
    }

    private static int correct(CommandSourceStack source, String businessId, long periodEnd, long revenue,
                               long expenses, String reason) {
        IndividualBusiness business = IndividualBusinessSavedData.get(source.getServer()).findByBusinessId(businessId);
        IndividualTaxPeriodSavedData periods = IndividualTaxPeriodSavedData.get(source.getServer());
        var oldEntry = periods.forBusiness(businessId).stream().filter(entry -> entry.periodEnd() == periodEnd).findFirst().orElse(null);
        long oldRevenue = oldEntry == null ? 0L : oldEntry.revenue();
        long oldExpenses = oldEntry == null ? 0L : oldEntry.expenses();
        if (business == null || oldEntry == null || !periods.correct(businessId, periodEnd, revenue, expenses)) {
            source.sendFailure(Component.literal("个人经营主体或税务周期不存在。"));
            return 0;
        }
        TaxSubject subject = new TaxSubject(TaxType.INDIVIDUAL_BUSINESS_INCOME, businessId, business.ownerUuid());
        TaxLedgerSavedData ledger = TaxLedgerSavedData.get(source.getServer());
        String originalSource = "individual-quarter:" + businessId + ":" + periodEnd;
        TaxBill original = ledger.findBySourceEvent(originalSource);
        long oldTax = original == null ? 0L : original.amount();
        long newProfit = Math.max(0L, revenue - Math.min(revenue, expenses));
        long newTax = TaxRuleService.taxMinor(source.getServer(), TaxType.INDIVIDUAL_BUSINESS_INCOME,
                Money.toMinorSaturated(newProfit), source.getServer().overworld().getGameTime());
        long difference = newTax - oldTax;
        String correctionSource = "individual-correction:" + businessId + ":" + periodEnd + ":" + source.getServer().overworld().getGameTime();
        if (difference > 0L) {
            TaxPeriod period = new TaxPeriod(Math.max(0L, periodEnd - 90L * 24000L), periodEnd,
                    periodEnd + 15L * 24000L, periodEnd + 30L * 24000L);
            TaxService.createPeriodicBill(source.getServer(), subject, "usd", difference, period,
                    Money.toMinorSaturated(newProfit), TaxRuleService.rateBasisPoints(source.getServer(), TaxType.INDIVIDUAL_BUSINESS_INCOME, periodEnd), correctionSource);
        } else if (difference < 0L) {
            com.ailudick.capitalismmod.tax.TaxCreditSavedData.get(source.getServer()).add(subject, "usd", -difference,
                    correctionSource, Math.max(0L, periodEnd - 90L * 24000L), periodEnd, source.getServer().overworld().getGameTime());
        }
        source.sendSuccess(() -> Component.literal("更正已记录：" + reason + " | 税额差额 " + Money.format(Math.abs(difference))), true);
        TaxCorrectionAuditSavedData.get(source.getServer()).add(new TaxCorrectionAuditSavedData.Entry(
                java.util.UUID.randomUUID().toString(), businessId, periodEnd, oldRevenue, oldExpenses,
                revenue, expenses, oldTax, newTax, difference, source.getTextName(),
                source.getServer().overworld().getGameTime(), reason));
        return 1;
    }

    private static int history(CommandSourceStack source) {
        var entries = TaxCorrectionAuditSavedData.get(source.getServer()).all();
        if (entries.isEmpty()) { source.sendSuccess(() -> Component.literal("暂无税务更正记录。"), false); return 0; }
        entries.stream().limit(20).forEach(entry -> source.sendSuccess(() -> Component.literal(
                entry.id() + " | business=" + entry.businessId() + " | periodEnd=" + entry.periodEnd()
                        + " | revenue " + entry.oldRevenue() + " -> " + entry.newRevenue()
                        + " | expenses " + entry.oldExpenses() + " -> " + entry.newExpenses()
                        + " | tax diff=" + Money.format(Math.abs(entry.difference()))
                        + " | by=" + entry.administrator() + " | " + entry.reason()), false));
        return entries.size();
    }
}
